package com.eplataforma_deliveri.user_service.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.eplataforma_deliveri.user_service.dtos.AuthResponseDto;
import com.eplataforma_deliveri.user_service.dtos.LoginRequestDto;
import com.eplataforma_deliveri.user_service.dtos.RegisterDto;
import com.eplataforma_deliveri.user_service.exceptions.InvalidCredentialsException;
import com.eplataforma_deliveri.user_service.models.User;
import com.eplataforma_deliveri.user_service.repositories.IUserRepository;

@Service
public class UserService {

    @Autowired
    private IUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public List<User> findAll() {
        return repository.findAll();
    }

    // Cambiar a AuthResponse cuando implemente los tokens
    public AuthResponseDto register(RegisterDto request) {
        if (repository.existsByEmail(request.email())) {
            throw new RuntimeException("El email ya esta en uso");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));

        User userSaved = repository.save(user);

        String token = jwtService.generateToken(userSaved.getEmail(), userSaved.getRol());
        return new AuthResponseDto(token, userSaved.getEmail(), userSaved.getRol());
    }

    public AuthResponseDto login(LoginRequestDto request) {
        User user = repository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRol());
        return new AuthResponseDto(token, user.getEmail(), user.getRol());
    }
}
