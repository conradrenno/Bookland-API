package com.devrenno.bookland.websupport.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * What a service needs to know to accept a Bookland access token — and nothing about issuing one.
 *
 * @param issuer    must equal the {@code iss} the Authorization Server writes
 * @param audience  the {@code aud} an access token for this API carries
 * @param jwkSetUri where to fetch the public keys from. Left unset in the monolith, where the keys
 *                  are read from the Authorization Server's own {@code JWKSource} in memory; set by a
 *                  service running in a process of its own, which can only reach them over HTTP
 */
@ConfigurationProperties("bookland.resource-server")
public record ResourceServerProperties(String issuer, String audience, String jwkSetUri) {
}
