package com.trsthales.ecommerce.identity.infrastructure;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Component
public class RsaKeyProvider {

    private final RsaKeyProperties properties;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public RsaKeyProvider(RsaKeyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.getPublicKey() != null && properties.getPrivateKey() != null) {
            this.publicKey = properties.getPublicKey();
            this.privateKey = properties.getPrivateKey();
            log.info("RSA KeyPair successfully loaded from configuration properties");
        } else {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                this.publicKey = (RSAPublicKey) keyPair.getPublic();
                this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
                log.info("RSA KeyPair generated in-memory (2048 bits) for development/test profile");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to generate RSA KeyPair", e);
            }
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }
}
