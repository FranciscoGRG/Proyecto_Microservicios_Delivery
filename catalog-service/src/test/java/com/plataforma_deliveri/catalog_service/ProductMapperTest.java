package com.plataforma_deliveri.catalog_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.models.Product;
import com.plataforma_deliveri.catalog_service.services.ProductMapper;

public class ProductMapperTest {
    private final String PRODUCT_ID = "12345";
    private final String NAME = "Laptop Gaming";
    private final String DESCRIPTION = "Potente laptop para juegos";
    private final Double PRICE = 1500.00;
    private final Integer STOCK = 10;
    private final String CATEGORY = "Electronics";
    private final LocalDate NOW = LocalDate.now();

    @Test
    void toEntity_ShouldMapDtoToProductEntity_AndSetDefaults() {
        ProductRequestDto dto = new ProductRequestDto(
                NAME,
                DESCRIPTION,
                PRICE,
                STOCK,
                CATEGORY);

        Product entity = ProductMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(NAME, entity.getName());
        assertEquals(DESCRIPTION, entity.getDescripction());
        assertEquals(PRICE, entity.getPrice());
        assertEquals(STOCK, entity.getStock());
        assertEquals(CATEGORY, entity.getCategory());
        assertTrue(entity.isActive(), "El campo 'active' debe ser true por defecto.");
    }

    @Test
    void toResponseDTO_ShouldMapProductEntityToResponseDto() {
        Product entity = new Product();
        entity.setId(PRODUCT_ID);
        entity.setName(NAME);
        entity.setDescripction(DESCRIPTION);
        entity.setPrice(PRICE);
        entity.setStock(STOCK);
        entity.setCategory(CATEGORY);
        entity.setCreatedAt(NOW);

        ProductResponseDto dto = ProductMapper.toResponseDTO(entity);

        assertNotNull(dto);
        assertEquals(PRODUCT_ID, dto.id());
        assertEquals(NAME, dto.name());
        assertEquals(DESCRIPTION, dto.description());
        assertEquals(PRICE, dto.price());
        assertEquals(STOCK, dto.stock());
        assertEquals(CATEGORY, dto.category());
        assertEquals(NOW, dto.createdAt());
    }
}
