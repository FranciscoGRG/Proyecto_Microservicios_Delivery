package com.eplataforma_deliveri.user_service.dtos;

public record UserDto(
    Long id,
    String name,
    String email,
    String phone
) {}
