package com.plataforma_deliveri.catalog_service.dtos;

public record ProductRequestDto(
    String name,
    String description,
    Double price,
    Integer stock,
    String category
) {}
