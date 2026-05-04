package com.finanzas.gestion_financiera.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.controller.AuthController;
import com.finanzas.gestion_financiera.dto.AuthResponse;
import com.finanzas.gestion_financiera.dto.LoginRequest;
import com.finanzas.gestion_financiera.dto.RegisterRequest;
import com.finanzas.gestion_financiera.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Feature - API /api/v1/auth")
class AuthControllerFeatureTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("Should register user and return 200 with token")
        void shouldRegisterUserSuccessfully() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            AuthResponse authResponse = new AuthResponse("jwt-token", "juan@email.com", "Juan");
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("juan@email.com"))
                    .andExpect(jsonPath("$.primer_nombre").value("Juan"));
        }

        @Test
        @DisplayName("Should return 400 when first name is empty")
        void shouldReturn400WhenFirstNameIsEmpty() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("no-es-email");
            request.setContrasena("Password1!");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password does not meet requirements")
        void shouldReturn400WhenPasswordIsWeak() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("simple");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when last name is missing")
        void shouldReturn400WhenLastNameIsMissing() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when body is empty")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should authenticate and return 200 with token")
        void shouldAuthenticateSuccessfully() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            AuthResponse authResponse = new AuthResponse("jwt-token", "juan@email.com", "Juan");
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("juan@email.com"));
        }

        @Test
        @DisplayName("Should return 400 when email is empty")
        void shouldReturn400WhenEmailIsEmpty() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setContrasena("Password1!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
