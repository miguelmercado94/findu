package com.findu.core.infrastructure.adapter.out.storage;

import com.findu.core.application.port.out.storage.ObjectStoragePort;
import com.findu.core.domain.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implementacion en memoria de {@link ObjectStoragePort} para tests. No es un bean de Spring.
 */
public class InMemoryObjectStorageAdapter implements ObjectStoragePort {

    private final Map<String, byte[]> objects = new TreeMap<>();
    private final Map<String, String> contentTypes = new TreeMap<>();

    @Override
    public synchronized void put(String key, byte[] bytes, String contentType) {
        objects.put(key, bytes.clone());
        contentTypes.put(key, contentType);
    }

    @Override
    public synchronized byte[] get(String key) {
        byte[] data = objects.get(key);
        if (data == null) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        return data.clone();
    }

    @Override
    public synchronized boolean exists(String key) {
        return objects.containsKey(key);
    }

    @Override
    public synchronized List<String> listKeys(String prefix) {
        return objects.keySet().stream().filter(k -> k.startsWith(prefix)).toList();
    }

    @Override
    public synchronized void copy(String sourceKey, String destinationKey) {
        put(destinationKey, get(sourceKey), contentTypes.get(sourceKey));
    }

    @Override
    public synchronized void delete(String key) {
        objects.remove(key);
        contentTypes.remove(key);
    }

    public synchronized String contentTypeOf(String key) {
        return contentTypes.get(key);
    }
}