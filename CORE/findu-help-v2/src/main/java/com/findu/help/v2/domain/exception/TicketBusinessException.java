package com.findu.help.v2.domain.exception;

public class TicketBusinessException extends HelpTicketException {

    public TicketBusinessException(String message) {
        super(message);
    }

    public TicketBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
