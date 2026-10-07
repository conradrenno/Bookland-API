package com.devrenno.bookland;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * The key the integration tests sign their tokens with, standing in for the identity service's.
 *
 * <p>This process has no Authorization Server any more; in production it fetches the identity
 * service's public keys over HTTP. MockMvc opens no port and the identity service is not running, so
 * {@code @BooklandIntegrationTest} blanks {@code jwk-set-uri} and the decoder falls back to the
 * {@code JWKSource} bean in the context — this one. Generated per context, so no key is committed.
 *
 * <p>What a key of our own cannot prove is that the tokens these tests mint look like the ones the
 * identity service issues. That half lives on the other side: its
 * {@code AuthorizationCodeFlowIntegrationTest} pins the claims it writes, and {@link TestAccessTokens}
 * must keep writing the same ones.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestSigningKey {

    @Bean
    JWKSource<SecurityContext> testJwkSource() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAKey key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("bookland-test-key")
                .build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }
}
