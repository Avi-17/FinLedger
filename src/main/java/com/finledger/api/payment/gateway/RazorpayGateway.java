package com.finledger.api.payment.gateway;

import com.finledger.api.common.PaymentGatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Translates our PaymentGateway calls into Razorpay SDK calls. The only class that imports com.razorpay. */
@Slf4j
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

    private static long toPaise(BigDecimal rupees) {
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
        return toEvent(type, p);
    }

    @Override
    public GatewayEvent fetchPayment(String paymentId) {
        try {
            JSONObject p = client.payments.fetch(paymentId).toJson();
            GatewayEvent.Type type = switch (p.optString("status")) {
                case "captured" -> GatewayEvent.Type.CAPTURED;
                case "failed"   -> GatewayEvent.Type.FAILED;
                default -> GatewayEvent.Type.IGNORED;      // created / authorized / refunded: nothing to record yet
            };
            return type == GatewayEvent.Type.IGNORED ? GatewayEvent.ignored() : toEvent(type, p);
        } catch (RazorpayException e) {
            log.warn("Could not fetch payment {} from Razorpay: {}", paymentId, e.getMessage());
            return GatewayEvent.ignored();                 // fall back to waiting for the webhook
        }
    }

    /** Razorpay payment entity (same shape in webhooks and in the Payments API) -> our GatewayEvent. */
    private static GatewayEvent toEvent(GatewayEvent.Type type, JSONObject p) {
        return new GatewayEvent(type, p.optString("order_id", null), p.getString("id"), p.getLong("amount"));
    }
}