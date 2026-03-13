package com.company.common.security.cert.exception;

/**
 * Thrown when the citizen certificate has been revoked (CRL/OCSP check failed).
 */
public class MoicaRevocationException extends MoicaLoginException {

    public MoicaRevocationException(String msg) {
        super(msg);
    }

    public MoicaRevocationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
