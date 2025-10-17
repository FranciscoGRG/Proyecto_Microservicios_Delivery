package com.eplataforma_deliveri.user_service.dtos;

public record LoginRequestDto (
    String email,
    String password
) {}
