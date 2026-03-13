package com.company.common.security.cert.exception;

/**
 * Thrown when the citizen certificate has expired.
 */
public class MoicaExpiredException extends MoicaLoginException {

    public MoicaExpiredException(String msg) {
        super(msg);
    }

    public MoicaExpiredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
