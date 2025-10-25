package com.plataforma_deliveri.order_service.dtos;

public record OrderItemResponseDto(
    String productId,
    Integer quantity,
    Double priceAtOrder,
    Double subtotal
) {}
