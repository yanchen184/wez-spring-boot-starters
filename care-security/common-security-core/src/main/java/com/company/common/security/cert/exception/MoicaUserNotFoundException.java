package com.company.common.security.cert.exception;

/**
 * Thrown when no local user matches the citizen certificate identity (cname + last4IDNO).
 */
public class MoicaUserNotFoundException extends MoicaLoginException {

    public MoicaUserNotFoundException(String msg) {
        super(msg);
    }

    public MoicaUserNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
