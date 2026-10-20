package com.findu.transaction.application.port.out.storage;

import java.io.InputStream;

public interface FileStoragePort {
    String storeFile(String key, InputStream content, String contentType, long length);
    String getFileUrl(String key);
    void deleteFile(String key);
}
