package com.plataforma_deliveri.order_service.dtos;

import java.util.List;

public record OrderRequestDto(
    List<OrderItemRequestDto> items
) {}
