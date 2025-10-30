package com.fran.users_service.app.dtos;

public record RegisterRequestDto(
    String name,
    String email,
    String phone,
    String password
) {}
