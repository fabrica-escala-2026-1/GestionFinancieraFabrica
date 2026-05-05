package com.finanzas.gestion_financiera.integration;

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
class AuthIntegrationTest {

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
        @DisplayName("Debe registrar usuario y retornar 200 con token")
        void debeRegistrarUsuarioExitosamente() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            AuthResponse authResponse = new AuthResponse("jwt-token", "juan@email.com", "Juan");
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("juan@email.com"))
                    .andExpect(jsonPath("$.primer_nombre").value("Juan"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el nombre está vacío")
        void debeRetornar400SiNombreVacio() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si el email es inválido")
        void debeRetornar400SiEmailInvalido() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("no-es-email");
            request.setContrasena("Password1!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si la contraseña no cumple requisitos")
        void debeRetornar400SiContrasenaDebil() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("simple");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si falta el apellido")
        void debeRetornar400SiFaltaApellido() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si el body está vacío")
        void debeRetornar400SiBodyVacio() throws Exception {
            // Arrange - body vacío

            // Act & Assert
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
        @DisplayName("Debe autenticar y retornar 200 con token")
        void debeAutenticarExitosamente() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            AuthResponse authResponse = new AuthResponse("jwt-token", "juan@email.com", "Juan");
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("juan@email.com"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el email está vacío")
        void debeRetornar400SiEmailVacio() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setContrasena("Password1!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si falta la contraseña")
        void debeRetornar400SiFaltaContrasena() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
