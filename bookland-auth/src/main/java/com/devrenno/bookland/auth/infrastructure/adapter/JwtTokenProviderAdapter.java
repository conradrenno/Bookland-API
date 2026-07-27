package com.devrenno.bookland.auth.infrastructure.adapter;

import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.domain.exception.InvalidTokenException;
import com.devrenno.bookland.auth.domain.exception.TokenExpiredException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.auth.infrastructure.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final JwtProperties jwtProperties;

    @Override
    public Token generate(String userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtProperties.getExpirationMs());

        String tokenValue = Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();

        return new Token(tokenValue, expiresAt, UUID.fromString(userId), email, role);
    }

    @Override
    public Token validate(String tokenValue) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(tokenValue)
                    .getPayload();

            Instant expiresAt = claims.getExpiration().toInstant();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            return new Token(tokenValue, expiresAt, userId, email, role);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(e);
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
