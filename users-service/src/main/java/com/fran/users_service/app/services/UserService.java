package com.fran.users_service.app.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fran.users_service.app.dtos.AuthResponseDto;
import com.fran.users_service.app.dtos.LoginRequestDto;
import com.fran.users_service.app.dtos.RegisterRequestDto;
import com.fran.users_service.app.dtos.UpdateUserDTO;
import com.fran.users_service.app.dtos.UserDTO;
import com.fran.users_service.app.models.User;
import com.fran.users_service.app.repositories.IUserRepository;

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

    public AuthResponseDto register(RegisterRequestDto request) {
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRol());
        return new AuthResponseDto(token, user.getEmail(), user.getRol());
    }

    public User getProfile(String email) {
        return repository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public UserDTO getProfileByEmail(String email) {
        return repository.findUserDTOByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public void deleteUser(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("El user no existe"));
        repository.delete(user);
    }

    public User updatedUser(Long id, User updatedUser) {
        User existingUser = repository.findById(id).orElseThrow(() -> new RuntimeException("El user no existe"));

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setRol(updatedUser.getRol());
        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));

        return repository.save(existingUser);
    }

    public User updateUserProfile(Long userIdFromToken, UpdateUserDTO updateDto) {
        User existingUser = repository.findById(userIdFromToken)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (updateDto.getName() != null && !updateDto.getName().isBlank()) {
            existingUser.setName(updateDto.getName());
        }

        if (updateDto.getNewPassword() != null && !updateDto.getNewPassword().isBlank()) {
            if (updateDto.getCurrentPassword() == null
                    || !passwordEncoder.matches(updateDto.getCurrentPassword(), existingUser.getPassword())) {
                throw new RuntimeException("La contraseña actual es incorrecta o esta vacia");
            }

            existingUser.setPassword(passwordEncoder.encode(updateDto.getNewPassword()));
        }

        return repository.save(existingUser);
    }

    public Optional<Long> findIdByEmail(String email) {
        return repository.findIdByEmail(email);
    }
}
