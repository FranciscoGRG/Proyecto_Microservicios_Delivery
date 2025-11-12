package com.fran.users_service.app.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fran.users_service.app.dtos.AuthResponseDto;
import com.fran.users_service.app.dtos.LoginRequestDto;
import com.fran.users_service.app.dtos.RegisterRequestDto;
import com.fran.users_service.app.dtos.UpdateUserDTO;
import com.fran.users_service.app.dtos.UserDTO;
import com.fran.users_service.app.models.User;
import com.fran.users_service.app.services.UserService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(service.login(request));
    }

    @PostMapping("/profile")
    public ResponseEntity<User> profile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.getProfile(userDetails.getUsername()));
    }

    @GetMapping("/profile/{email}")
    public ResponseEntity<UserDTO> getProfileByEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.getProfileByEmail(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            service.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<User> updateUser(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateUserDTO updatedUser) {
        String userEmail = userDetails.getUsername();
        Optional<Long> userIdOptional = service.findIdByEmail(userEmail);

        if (userIdOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Long userId = userIdOptional.get();

        try {
            return ResponseEntity.ok(service.updateUserProfile(userId, updatedUser));
        }

        catch (RuntimeException e) {
            System.err.println("Error de lógica de negocio: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

}
