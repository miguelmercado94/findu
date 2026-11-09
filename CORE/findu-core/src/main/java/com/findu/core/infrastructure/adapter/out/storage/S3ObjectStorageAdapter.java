package com.findu.core.infrastructure.adapter.out.storage;

import com.findu.core.application.port.out.storage.ObjectStoragePort;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.exception.StorageException;
import com.findu.core.infrastructure.config.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;

@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageAdapter.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageAdapter(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getBucket();
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            throw fail("put", e);
        }
    }

    @Override
    public byte[] get(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Archivo no encontrado");
            }
            throw fail("get", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw fail("exists", e);
        }
    }

    @Override
    public List<String> listKeys(String prefix) {
        try {
            List<String> keys = new ArrayList<>();
            String token = null;
            do {
                ListObjectsV2Response res = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket).prefix(prefix).continuationToken(token).build());
                for (S3Object o : res.contents()) {
                    keys.add(o.key());
                }
                token = Boolean.TRUE.equals(res.isTruncated()) ? res.nextContinuationToken() : null;
            } while (token != null);
            return keys;
        } catch (S3Exception e) {
            throw fail("list", e);
        }
    }

    @Override
    public void copy(String sourceKey, String destinationKey) {
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket).sourceKey(sourceKey)
                    .destinationBucket(bucket).destinationKey(destinationKey).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Archivo no encontrado");
            }
            throw fail("copy", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw fail("delete", e);
        }
    }

    /** No se loguea la clave completa (contiene el documento del proveedor). */
    private StorageException fail(String op, S3Exception e) {
        log.error("Fallo S3 operacion={} status={} code={}", op, e.statusCode(),
                e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "n/a");
        return new StorageException("Error de almacenamiento (" + op + ")", e);
    }
}