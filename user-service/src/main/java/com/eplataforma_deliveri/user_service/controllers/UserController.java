package com.eplataforma_deliveri.user_service.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eplataforma_deliveri.user_service.dtos.LoginRequestDto;
import com.eplataforma_deliveri.user_service.dtos.RegisterDto;
import com.eplataforma_deliveri.user_service.models.User;
import com.eplataforma_deliveri.user_service.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/findAll")
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    //Cambiar a tipo AuthResponse cuando implemente los tokens
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterDto request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequestDto request) {      
        return ResponseEntity.ok(service.login(request));
    }
    
    

}
