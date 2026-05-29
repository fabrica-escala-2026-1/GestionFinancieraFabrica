package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.AuthResponse;
import com.finanzas.gestion_financiera.dto.LoginRequest;
import com.finanzas.gestion_financiera.dto.RegisterRequest;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryInitService categoryInitService;

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateKeyException("El email ya se encuentra registrado");
        }

        User user = new User();
        user.setPrimer_nombre(request.getPrimer_nombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setContrasena(passwordEncoder.encode(request.getContrasena()));

        usuarioRepository.save(user);
        categoryInitService.crearCategoriasPorDefecto(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getPrimer_nombre());
    }

    public AuthResponse login(LoginRequest request) {
        User user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getContrasena(), user.getContrasena())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getPrimer_nombre());
    }
}