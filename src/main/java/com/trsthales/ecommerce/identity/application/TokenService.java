package com.trsthales.ecommerce.identity.application;

import com.trsthales.ecommerce.identity.api.dto.TokenResponse;
import com.trsthales.ecommerce.identity.domain.Role;
import com.trsthales.ecommerce.identity.domain.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    public static final Duration TOKEN_EXPIRATION = Duration.ofHours(2);
    public static final String ISSUER = "ecommerce-backend";

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public TokenResponse generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_EXPIRATION);

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getId)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("user_id", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roleNames)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(tokenValue, "Bearer", TOKEN_EXPIRATION.toSeconds());
    }
}
