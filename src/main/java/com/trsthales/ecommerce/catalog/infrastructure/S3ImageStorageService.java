package com.trsthales.ecommerce.catalog.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;

    public S3ImageStorageService(
            S3Client s3Client,
            @Value("${cloud.aws.s3.bucket-name:ecommerce-catalog}") String bucketName,
            @Value("${cloud.aws.s3.endpoint:http://localhost:9000}") String endpoint
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    @Override
    public String uploadImage(String filename, byte[] content, String contentType) {
        String key = "products/" + UUID.randomUUID() + "-" + filename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));

        String sanitizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return sanitizedEndpoint + "/" + bucketName + "/" + key;
    }
}
