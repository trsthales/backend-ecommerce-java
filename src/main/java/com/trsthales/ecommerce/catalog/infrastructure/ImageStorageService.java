package com.trsthales.ecommerce.catalog.infrastructure;

public interface ImageStorageService {

    String uploadImage(String filename, byte[] content, String contentType);
}
