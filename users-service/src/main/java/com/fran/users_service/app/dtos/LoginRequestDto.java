package com.fran.users_service.app.dtos;

public record LoginRequestDto(
    String email,
    String password
) {}
