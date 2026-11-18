package com.finledger.api.common;

/** Thrown when a request breaks a business rule (invalid split, illegal status change...). */
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) { super(message); }
}
