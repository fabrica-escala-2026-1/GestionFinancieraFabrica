package com.finanzas.gestion_financiera.unit.config;

import com.finanzas.gestion_financiera.config.JwtAuthFilter;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter - Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should continue without authentication when Authorization header is missing")
    void shouldContinueWithoutAuthWhenHeaderMissing() throws ServletException, IOException {
        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue without authentication when header does not start with Bearer")
    void shouldContinueWhenHeaderIsNotBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic abc123");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue without authentication when token is invalid")
    void shouldContinueWhenTokenIsInvalid() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should set authentication when token is valid and user exists")
    void shouldSetAuthenticationWhenTokenIsValid() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("test@email.com");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@email.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Should continue without authentication when user does not exist in DB")
    void shouldContinueWhenUserDoesNotExistInDb() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("ghost@email.com");
        when(userRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
