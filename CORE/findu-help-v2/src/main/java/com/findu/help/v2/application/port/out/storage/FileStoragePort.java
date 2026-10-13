package com.findu.help.v2.application.port.out.storage;

import com.findu.help.v2.domain.valueobject.S3ObjectReference;
import com.findu.help.v2.domain.valueobject.TicketCode;

import java.io.InputStream;

public interface FileStoragePort {

    S3ObjectReference upload(FileUpload file);

    S3ObjectReference storeImage(TicketCode ticketCode, String fileName, String mimeType, InputStream content, long fileSizeBytes);

    void delete(String objectKey);
}
