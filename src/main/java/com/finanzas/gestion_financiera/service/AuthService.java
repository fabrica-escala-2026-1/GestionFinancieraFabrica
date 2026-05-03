package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.AuthResponse;
import com.finanzas.gestion_financiera.dto.LoginRequest;
import com.finanzas.gestion_financiera.dto.RegisterRequest;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryInitService categoryInitService;

    public AuthResponse register(RegisterRequest request) {

        // Si el email ya existe, el GlobalExceptionHandler convierte
        // este RuntimeException en: { "mensaje": "El nombre de usuario ya se encuentra registrado" }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado");
        }

        User user = new User();
        user.setPrimer_nombre(request.getPrimer_nombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setContrasena(passwordEncoder.encode(request.getContrasena()));

        userRepository.save(user);
        categoryInitService.crearCategoriasPorDefecto(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getPrimer_nombre());
    }

    public AuthResponse login(LoginRequest request) {

        // Email no encontrado → misma respuesta genérica que contraseña incorrecta
        // para no revelar si el email existe o no (buena práctica de seguridad)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // Contraseña incorrecta → mismo mensaje
        if (!passwordEncoder.matches(request.getContrasena(), user.getContrasena())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getPrimer_nombre());
    }
}