package com.unicornt.store.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Issues and verifies the HMAC signed access tokens used by the API.
 *
 * <p>The signing key is derived from {@code app.jwt.secret}, which is a Base64 encoded
 * value of at least 32 bytes supplied through the environment. There is deliberately no
 * fallback value: a missing secret must fail startup rather than silently sign tokens
 * with a well known key.</p>
 */
@Service
public class JwtService {

    /** Name of the claim carrying the granted authorities of the subject. */
    public static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final long expirationMs;

    JwtService(@Value("${app.jwt.secret}") String secret,
               @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Builds a signed token for the given subject.
     *
     * @param subject the account identity, the user email in this application
     * @param roles   authority names, already prefixed with {@code ROLE_}
     */
    public String generate(String subject, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(ROLES_CLAIM, List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry of a token.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired or badly signed
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    /** Token lifetime in milliseconds, echoed to clients as {@code expiresIn}. */
    public long expirationMs() {
        return expirationMs;
    }
}
