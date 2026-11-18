package com.finledger.api.common;

/** Thrown when the caller is not allowed to perform the action. */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) { super(message); }
}
