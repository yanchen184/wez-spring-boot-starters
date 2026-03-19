package com.company.common.security.cert;

/**
 * Thrown when certificate verification fails (expired, revoked, chain invalid, etc.).
 */
public class CertVerificationException extends RuntimeException {

    public CertVerificationException(String message) {
        super(message);
    }

    public CertVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
