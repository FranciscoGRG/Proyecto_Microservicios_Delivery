package com.fran.users_service.app.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fran.users_service.app.services.JwtService;

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {
            Claims claims = jwtService.extractAllClaims(token);
            return ResponseEntity.ok(claims.getSubject());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
