package com.plataforma_deliveri.order_service.dtos;

public record ProductDto(
    String id,
    String name,
    String description,
    Double price,
    Integer stock,
    String category
) {}
