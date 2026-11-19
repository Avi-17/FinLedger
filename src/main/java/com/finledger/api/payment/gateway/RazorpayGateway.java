package com.finledger.api.payment.gateway;

import com.finledger.api.common.PaymentGatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Translates our PaymentGateway calls into Razorpay SDK calls. The only class that imports com.razorpay. */
@Component
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayGateway implements PaymentGateway {
    private final RazorpayProperties props;
    private final RazorpayClient client;

    public RazorpayGateway(RazorpayProperties props) throws RazorpayException {
        this.props = props;
        this.client = new RazorpayClient(props.keyId(), props.keySecret());
    }

    @Override
    public String createOrder(BigDecimal amount, String receipt) {
        JSONObject req = new JSONObject();
        req.put("amount", toPaise(amount));     // Razorpay wants the smallest unit: ₹107.50 -> 10750
        req.put("currency", "INR");
        req.put("receipt", receipt);
        try {
            Order order = client.orders.create(req);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new PaymentGatewayException("Could not create payment order: " + e.getMessage());
        }
    }

    @Override
    public boolean isPaymentSignatureValid(String orderId, String paymentId, String signature) {
        JSONObject attrs = new JSONObject();
        attrs.put("razorpay_order_id", orderId);
        attrs.put("razorpay_payment_id", paymentId);
        attrs.put("razorpay_signature", signature);
        try {
            return Utils.verifyPaymentSignature(attrs, props.keySecret());
        } catch (RazorpayException e) {
            return false;
        }
    }

    @Override
    public boolean isWebhookSignatureValid(String rawBody, String signature) {
        if (signature == null) return false;
        try {
            return Utils.verifyWebhookSignature(rawBody, signature, props.webhookSecret());
        } catch (RazorpayException e) {
            return false;
        }
    }

    @Override
    public String publicKey() { return props.keyId(); }

    public static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }

    @Override
    public GatewayEvent parseWebhook(String rawBody) {
        JSONObject root = new JSONObject(rawBody);
        GatewayEvent.Type type = switch (root.optString("event")) {
            case "payment.captured" -> GatewayEvent.Type.CAPTURED;
            case "payment.failed"   -> GatewayEvent.Type.FAILED;
            default -> GatewayEvent.Type.IGNORED;
        };
        if (type == GatewayEvent.Type.IGNORED) return GatewayEvent.ignored();

        JSONObject p = root.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        return new GatewayEvent(type, p.getString("order_id"), p.getString("id"), p.getLong("amount"));
    }
}