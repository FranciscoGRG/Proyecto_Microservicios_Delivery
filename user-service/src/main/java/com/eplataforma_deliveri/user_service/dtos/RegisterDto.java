package com.eplataforma_deliveri.user_service.dtos;

public record RegisterDto (
    String name,
    String email,
    String phone,
    String password
) {} 
