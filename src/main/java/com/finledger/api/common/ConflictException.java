package com.finledger.api.common;

/** Thrown when the request clashes with existing data (e.g. email already registered). */
public class ConflictException extends DomainException {
    public ConflictException(String message) { super(message); }
}
