package com.plataforma_deliveri.catalog_service.dtos;

import java.time.LocalDate;

public record ProductResponseDto(
    String id,
    String name,
    String description,
    Double price,
    Integer stock,
    String category,
    LocalDate createdAt
) {}
