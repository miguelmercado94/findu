package com.findu.help.v2.domain.exception;

public abstract class HelpTicketException extends RuntimeException {

    public HelpTicketException(String message) {
        super(message);
    }

    public HelpTicketException(String message, Throwable cause) {
        super(message, cause);
    }
}
