package com.trsthales.ecommerce.catalog.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${cloud.aws.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${cloud.aws.s3.region:us-east-1}") String region,
            @Value("${cloud.aws.s3.access-key:minioadmin}") String accessKey,
            @Value("${cloud.aws.s3.secret-key:minioadmin}") String secretKey
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true)
                .build();
    }
}
