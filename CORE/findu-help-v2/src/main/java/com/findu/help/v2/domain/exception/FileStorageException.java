package com.findu.help.v2.domain.exception;

public class FileStorageException extends HelpTicketException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
