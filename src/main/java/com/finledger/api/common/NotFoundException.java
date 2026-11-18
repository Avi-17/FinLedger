package com.finledger.api.common;

/** Thrown when a requested resource does not exist. */
public class NotFoundException extends DomainException {
    public NotFoundException(String message) { super(message); }
}
