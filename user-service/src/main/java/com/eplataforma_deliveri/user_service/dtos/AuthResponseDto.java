package com.eplataforma_deliveri.user_service.dtos;

public record AuthResponseDto (
    String token,
    String email,
    String rol
) {}
