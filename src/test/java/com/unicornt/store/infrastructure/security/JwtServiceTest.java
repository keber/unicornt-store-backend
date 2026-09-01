package com.unicornt.store.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract of the issued token: subject, roles claim and rejection of foreign or stale tokens. */
class JwtServiceTest {

    private static String randomSecret() {
        byte[] material = new byte[32];
        new SecureRandom().nextBytes(material);
        return Base64.getEncoder().encodeToString(material);
    }

    @Test
    void tokenCarriesSubjectAndRolesClaim() {
        JwtService service = new JwtService(randomSecret(), 60_000L);

        var claims = service.parse(service.generate("ada@example.com", List.of("ROLE_USER"))).getPayload();

        assertEquals("ada@example.com", claims.getSubject());
        assertEquals(List.of("ROLE_USER"), claims.get(JwtService.ROLES_CLAIM, List.class));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService issuer = new JwtService(randomSecret(), 60_000L);
        JwtService verifier = new JwtService(randomSecret(), 60_000L);
        String token = issuer.generate("ada@example.com", List.of("ROLE_USER"));

        assertThrows(SignatureException.class, () -> verifier.parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(randomSecret(), -1_000L);
        String token = service.generate("ada@example.com", List.of("ROLE_USER"));

        assertThrows(ExpiredJwtException.class, () -> service.parse(token));
    }

    @Test
    void lifetimeIsReportedToClients() {
        assertEquals(60_000L, new JwtService(randomSecret(), 60_000L).expirationMs());
    }
}
