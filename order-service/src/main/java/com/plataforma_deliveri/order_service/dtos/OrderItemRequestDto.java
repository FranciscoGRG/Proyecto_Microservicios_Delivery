package com.plataforma_deliveri.order_service.dtos;

public record OrderItemRequestDto(
    String id,
    Integer quantity
) {}
