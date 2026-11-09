package com.findu.core.application.port.out.storage;

import java.util.List;

/**
 * Puerto de salida para almacenamiento de objetos (S3). Opera sobre claves ya construidas
 * (ver {@code ProviderStoragePaths}).
 */
public interface ObjectStoragePort {

    void put(String key, byte[] bytes, String contentType);

    /** @throws com.findu.core.domain.exception.ResourceNotFoundException si la clave no existe */
    byte[] get(String key);

    boolean exists(String key);

    /** Claves que comienzan con el prefijo dado (puede estar vacia). */
    List<String> listKeys(String prefix);

    void copy(String sourceKey, String destinationKey);

    void delete(String key);
}