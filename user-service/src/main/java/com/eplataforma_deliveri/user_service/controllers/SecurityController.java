package com.eplataforma_deliveri.user_service.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eplataforma_deliveri.user_service.services.JwtService;

import io.jsonwebtoken.Claims;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityController {

    private final JwtService jwtService;

    public SecurityController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/validate-token")
    public ResponseEntity<String> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Este caso lo debería manejar el Gateway, pero es bueno tener la defensa aquí
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        String token = authHeader.substring(7);

        // La validación ahora solo se centra en si el token es estructuralmente válido y no expirado.
        if (jwtService.isTokenValid(token)) {
            Claims claims = jwtService.extractAllClaims(token);
            // Devolvemos el Subject (email) si es válido
            return ResponseEntity.ok(claims.getSubject()); 
        } else {
            // CRÍTICO: Si el token falla (expirado, firma incorrecta), devolvemos 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Validation-Error", "Invalid or Expired Token")
                    .build();
        }
    }
}
