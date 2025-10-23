package com.plataforma_deliveri.catalog_service.services;

import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.models.Product;

public class ProductMapper {

    public static Product toEntity(ProductRequestDto dto) {
        Product product = new Product();

        product.setName(dto.name());
        product.setDescripction(dto.description()); 
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setCategory(dto.category());
        product.setActive(true);

        return product;
    }

    public static ProductResponseDto toResponseDTO(Product entity) {
        return new ProductResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescripction(),
                entity.getPrice(),
                entity.getStock(),
                entity.getCategory(),
                entity.getCreatedAt());
    }
}
