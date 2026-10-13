package com.findu.help.v2.application.port.out.storage;

import com.findu.help.v2.domain.valueobject.TicketCode;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

@Getter
@Builder
public class FileUpload {
    private final TicketCode ticketCode;
    private final String fileName;
    private final String mimeType;
    private final InputStream content;
    private final long fileSizeBytes;

    public FileUpload(TicketCode ticketCode, String fileName, String mimeType, InputStream content, long fileSizeBytes) {
        if (ticketCode == null) {
            throw new IllegalArgumentException("El ticketCode es obligatorio para subir un archivo");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo es obligatorio");
        }
        if (content == null) {
            throw new IllegalArgumentException("El contenido del archivo no puede ser nulo");
        }
        this.ticketCode = ticketCode;
        this.fileName = fileName.trim();
        this.mimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType.trim() : "application/octet-stream";
        this.content = content;
        this.fileSizeBytes = fileSizeBytes;
    }
}
