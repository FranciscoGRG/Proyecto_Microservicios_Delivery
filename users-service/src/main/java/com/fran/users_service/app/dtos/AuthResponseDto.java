package com.fran.users_service.app.dtos;

public record AuthResponseDto (
    String token,
    String email,
    String rol
) {}
