package com.finledger.api.common;

/** Thrown when login credentials are wrong. */
public class AuthenticationFailedException extends DomainException {
    public AuthenticationFailedException(String message) { super(message); }
}
