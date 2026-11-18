package com.finledger.api.common;

/** Base for business-rule failures. Knows nothing about HTTP; GlobalExceptionHandler maps subclasses to status codes. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}
