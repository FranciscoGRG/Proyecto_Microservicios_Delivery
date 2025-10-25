package com.plataforma_deliveri.order_service.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
    Long id,
    String userEmail,
    String status,
    Double totalPrice,
    LocalDateTime createdAt,
    List<OrderItemResponseDto> items
) {}
