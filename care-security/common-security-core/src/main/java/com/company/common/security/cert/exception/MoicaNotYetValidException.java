package com.company.common.security.cert.exception;

/**
 * Thrown when the citizen certificate is not yet valid (future start date).
 */
public class MoicaNotYetValidException extends MoicaLoginException {

    public MoicaNotYetValidException(String msg) {
        super(msg);
    }

    public MoicaNotYetValidException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
