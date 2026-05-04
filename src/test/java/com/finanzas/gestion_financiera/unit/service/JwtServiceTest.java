package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService - Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "clave-super-secreta-larga-para-jwt-minimo-32-caracteres-test");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("Should generate a non-null JWT token")
        void shouldGenerateNonNullToken() {
            String email = "test@email.com";

            String token = jwtService.generateToken(email);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should generate different tokens for different emails")
        void shouldGenerateDifferentTokens() {
            String email1 = "user1@email.com";
            String email2 = "user2@email.com";

            String token1 = jwtService.generateToken(email1);
            String token2 = jwtService.generateToken(email2);

            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("Token should have valid JWT format (3 dot-separated parts)")
        void tokenShouldHaveJwtFormat() {
            String email = "test@email.com";

            String token = jwtService.generateToken(email);

            String[] parts = token.split("\\.");
            assertEquals(3, parts.length, "JWT must have 3 parts: header.payload.signature");
        }
    }

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("Should extract the correct email from the token")
        void shouldExtractCorrectEmail() {
            String email = "juan@email.com";
            String token = jwtService.generateToken(email);

            String extractedEmail = jwtService.extractEmail(token);

            assertEquals(email, extractedEmail);
        }

        @Test
        @DisplayName("Should throw exception when token is invalid")
        void shouldThrowExceptionWithInvalidToken() {
            String invalidToken = "token.invalido.aqui";

            assertThrows(Exception.class,
                    () -> jwtService.extractEmail(invalidToken));
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("Should return true for a valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateToken("test@email.com");

            boolean result = jwtService.isTokenValid(token);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false for an invalid token")
        void shouldReturnFalseForInvalidToken() {
            String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid.signature";

            boolean result = jwtService.isTokenValid(invalidToken);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for an expired token")
        void shouldReturnFalseForExpiredToken() {
            JwtService expiredJwtService = new JwtService();
            ReflectionTestUtils.setField(expiredJwtService, "secret",
                    "clave-super-secreta-larga-para-jwt-minimo-32-caracteres-test");
            ReflectionTestUtils.setField(expiredJwtService, "expiration", -1000L);

            String token = expiredJwtService.generateToken("test@email.com");

            boolean result = expiredJwtService.isTokenValid(token);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for a token with a different signature")
        void shouldReturnFalseForTokenWithDifferentSignature() {
            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "secret",
                    "otra-clave-secreta-diferente-para-jwt-minimo-32-caracteres");
            ReflectionTestUtils.setField(otherService, "expiration", 86400000L);

            String token = otherService.generateToken("test@email.com");

            boolean result = jwtService.isTokenValid(token);

            assertFalse(result);
        }
    }
}
