package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.AuthResponse;
import com.finanzas.gestion_financiera.dto.LoginRequest;
import com.finanzas.gestion_financiera.dto.RegisterRequest;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.AuthService;
import com.finanzas.gestion_financiera.service.CategoryInitService;
import com.finanzas.gestion_financiera.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CategoryInitService categoryInitService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Debe registrar usuario exitosamente con datos válidos")
        void debeRegistrarUsuarioExitosamente() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Juan");
            request.setApellido("Pérez");
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            when(usuarioRepository.findByEmail("juan@email.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("Password1!")).thenReturn("encoded-password");
            when(usuarioRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken("juan@email.com")).thenReturn("jwt-token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("juan@email.com", response.getEmail());
            assertEquals("Juan", response.getPrimer_nombre());
            verify(usuarioRepository).save(any(User.class));
            verify(categoryInitService).crearCategoriasPorDefecto(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el email ya está registrado")
        void debeLanzarExcepcionSiEmailDuplicado() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existente@email.com");

            User existingUser = new User();
            existingUser.setEmail("existente@email.com");
            when(usuarioRepository.findByEmail("existente@email.com")).thenReturn(Optional.of(existingUser));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.register(request));
            assertEquals("El email ya se encuentra registrado", exception.getMessage());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe encriptar la contraseña antes de guardar")
        void debeEncriptarContrasena() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Ana");
            request.setApellido("López");
            request.setEmail("ana@email.com");
            request.setContrasena("MiPass1!");

            when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("MiPass1!")).thenReturn("$2a$encoded");
            when(usuarioRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(anyString())).thenReturn("token");

            // Act
            authService.register(request);

            // Assert
            verify(passwordEncoder).encode("MiPass1!");
            verify(usuarioRepository).save(argThat(user ->
                    "$2a$encoded".equals(user.getContrasena())
            ));
        }

        @Test
        @DisplayName("Debe crear categorías por defecto al registrar usuario")
        void debeCrearCategoriasPorDefecto() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setPrimer_nombre("Carlos");
            request.setApellido("García");
            request.setEmail("carlos@email.com");
            request.setContrasena("Pass123!");

            when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(usuarioRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(anyString())).thenReturn("token");

            // Act
            authService.register(request);

            // Assert
            verify(categoryInitService).crearCategoriasPorDefecto(any(User.class));
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Debe autenticar usuario con credenciales correctas")
        void debeAutenticarConCredencialesCorrectas() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("Password1!");

            User user = new User();
            user.setId(1L);
            user.setEmail("juan@email.com");
            user.setPrimer_nombre("Juan");
            user.setContrasena("$2a$encoded");

            when(usuarioRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password1!", "$2a$encoded")).thenReturn(true);
            when(jwtService.generateToken("juan@email.com")).thenReturn("jwt-token");

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("juan@email.com", response.getEmail());
            assertEquals("Juan", response.getPrimer_nombre());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el email no existe")
        void debeLanzarExcepcionSiEmailNoExiste() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("noexiste@email.com");
            request.setContrasena("Password1!");

            when(usuarioRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.login(request));
            assertEquals("Credenciales inválidas", exception.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar excepción si la contraseña es incorrecta")
        void debeLanzarExcepcionSiContrasenaIncorrecta() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@email.com");
            request.setContrasena("WrongPass1!");

            User user = new User();
            user.setEmail("juan@email.com");
            user.setContrasena("$2a$encoded");

            when(usuarioRepository.findByEmail("juan@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPass1!", "$2a$encoded")).thenReturn(false);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.login(request));
            assertEquals("Credenciales inválidas", exception.getMessage());
        }
    }
}
