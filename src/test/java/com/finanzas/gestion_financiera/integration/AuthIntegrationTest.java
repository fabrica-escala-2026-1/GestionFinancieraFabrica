package com.finanzas.gestion_financiera.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.controller.AuthController;
import com.finanzas.gestion_financiera.dto.AuthResponse;
import com.finanzas.gestion_financiera.dto.LoginRequest;
import com.finanzas.gestion_financiera.dto.RegisterRequest;
import com.finanzas.gestion_financiera.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Auth Integration Tests for SonarQube Coverage")
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

    @Test // AF-01
    @DisplayName("POST /api/v1/auth/register - Registro exitoso")
    void registrarUsuario_Exito() throws Exception {
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

    @Test // AF-02
    @DisplayName("POST /api/v1/auth/register - Error por nombre vacío")
    void registrarUsuario_NombreVacio() throws Exception {
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

    @Test // AF-03
    @DisplayName("POST /api/v1/auth/register - Error por email inválido")
    void registrarUsuario_EmailInvalido() throws Exception {
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

    @Test // AF-04
    @DisplayName("POST /api/v1/auth/register - Error por contraseña débil")
    void registrarUsuario_ContrasenaDebil() throws Exception {
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

    @Test // AF-05
    @DisplayName("POST /api/v1/auth/register - Error por apellido vacío")
    void registrarUsuario_ApellidoVacio() throws Exception {
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

    @Test // AF-06
    @DisplayName("POST /api/v1/auth/register - Error por body vacío")
    void registrarUsuario_BodyVacio() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test // AF-07
    @DisplayName("POST /api/v1/auth/login - Login exitoso")
    void login_Exito() throws Exception {
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

    @Test // AF-08
    @DisplayName("POST /api/v1/auth/login - Error por email vacío")
    void login_EmailVacio() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setContrasena("Password1!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test // AF-09
    @DisplayName("POST /api/v1/auth/login - Error por contraseña vacía")
    void login_ContrasenaVacia() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@email.com");
        request.setContrasena("");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}