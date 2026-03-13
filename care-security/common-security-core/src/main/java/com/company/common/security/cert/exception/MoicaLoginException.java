package com.company.common.security.cert.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Base exception for all MOICA citizen certificate login failures.
 */
public class MoicaLoginException extends BadCredentialsException {

    public MoicaLoginException(String msg) {
        super(msg);
    }

    public MoicaLoginException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
