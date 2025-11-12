package com.plataforma_deliveri.catalog_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.errors.ProductNotFoundException;
import com.plataforma_deliveri.catalog_service.repositories.IProductRepository;
import com.plataforma_deliveri.catalog_service.services.ProductMapper;
import com.plataforma_deliveri.catalog_service.services.ProductService;
import com.plataforma_deliveri.catalog_service.models.Product;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private IProductRepository repository;

    @InjectMocks
    private ProductService productService;

    private ProductRequestDto requestDto;
    private Product productEntity;
    private final String PRODUCT_ID = "test-id-1";
    private final String NAME = "Test Product";
    private final Double PRICE = 10.50;

    @BeforeEach
    void setUp() {
        requestDto = new ProductRequestDto(
                NAME,
                "Description 1",
                PRICE,
                5,
                "Category A");

        productEntity = new Product();
        productEntity.setId(PRODUCT_ID);
        productEntity.setName(NAME);
        productEntity.setDescripction("Description 1");
        productEntity.setPrice(PRICE);
        productEntity.setStock(5);
        productEntity.setCategory("Category A");
        productEntity.setCreatedAt(LocalDate.now());
    }

    @Test
    void createProduct_ShouldSaveAndReturnResponseDto() {
        try (MockedStatic<ProductMapper> mockedMapper = mockStatic(ProductMapper.class)) {
            mockedMapper.when(() -> ProductMapper.toEntity(requestDto)).thenReturn(productEntity);

            when(repository.save(any(Product.class))).thenReturn(productEntity);

            ProductResponseDto expectedResponse = new ProductResponseDto(
                    PRODUCT_ID, NAME, "Description 1", PRICE, 5, "Category A", productEntity.getCreatedAt());
            mockedMapper.when(() -> ProductMapper.toResponseDTO(productEntity)).thenReturn(expectedResponse);

            ProductResponseDto result = productService.createProduct(requestDto);

            assertNotNull(result);
            assertEquals(PRODUCT_ID, result.id());
            verify(repository, times(1)).save(productEntity);
        }
    }

    @Test
    void findById_ShouldReturnProductResponseDto_WhenFound() {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));

        try (MockedStatic<ProductMapper> mockedMapper = mockStatic(ProductMapper.class)) {
            ProductResponseDto expectedResponse = new ProductResponseDto(
                    PRODUCT_ID, NAME, productEntity.getDescripction(), PRICE, productEntity.getStock(),
                    productEntity.getCategory(), productEntity.getCreatedAt());
            mockedMapper.when(() -> ProductMapper.toResponseDTO(productEntity)).thenReturn(expectedResponse);

            ProductResponseDto result = productService.findById(PRODUCT_ID);

            assertNotNull(result);
            assertEquals(PRODUCT_ID, result.id());
            verify(repository, times(1)).findById(PRODUCT_ID);
        }
    }

    @Test
    void findById_ShouldThrowRuntimeException_WhenNotFound() {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.findById(PRODUCT_ID);
        });
        assertEquals("Producto no encontrado", exception.getMessage());
        verify(repository, times(1)).findById(PRODUCT_ID);
    }

    @Test
    void getAllProducts_ShouldReturnListOfResponseDtos() {
        Product product2 = new Product();
        product2.setId("test-id-2");
        product2.setName("Product 2");

        List<Product> products = Arrays.asList(productEntity, product2);
        when(repository.findAll()).thenReturn(products);

        try (MockedStatic<ProductMapper> mockedMapper = mockStatic(ProductMapper.class)) {
            ProductResponseDto dto1 = new ProductResponseDto(
                    PRODUCT_ID, NAME, "d1", PRICE, 5, "c1", LocalDate.now());
            ProductResponseDto dto2 = new ProductResponseDto(
                    "test-id-2", "Product 2", "d2", PRICE, 5, "c1", LocalDate.now());

            mockedMapper.when(() -> ProductMapper.toResponseDTO(productEntity)).thenReturn(dto1);
            mockedMapper.when(() -> ProductMapper.toResponseDTO(product2)).thenReturn(dto2);

            List<ProductResponseDto> result = productService.getAllProducts();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(NAME, result.get(0).name());
            verify(repository, times(1)).findAll();
        }
    }

    @Test
    void updateProduct_ShouldUpdateAndReturnResponseDto_WhenFound() {
        ProductRequestDto updateRequest = new ProductRequestDto(
                "Updated Name",
                "Updated Description",
                20.00,
                15,
                "Category B");

        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));

        Product updatedProductEntity = new Product();
        updatedProductEntity.setId(PRODUCT_ID);
        updatedProductEntity.setName(updateRequest.name());
        when(repository.save(any(Product.class))).thenReturn(updatedProductEntity);

        try (MockedStatic<ProductMapper> mockedMapper = mockStatic(ProductMapper.class)) {
            ProductResponseDto expectedResponse = new ProductResponseDto(
                    PRODUCT_ID, updateRequest.name(), updateRequest.description(), updateRequest.price(),
                    updateRequest.stock(), updateRequest.category(), productEntity.getCreatedAt());
            mockedMapper.when(() -> ProductMapper.toResponseDTO(updatedProductEntity)).thenReturn(expectedResponse);

            ProductResponseDto result = productService.updateProduct(PRODUCT_ID, updateRequest);

            assertNotNull(result);
            assertEquals("Updated Name", result.name());
            verify(repository, times(1)).findById(PRODUCT_ID);
            verify(repository, times(1)).save(any(Product.class));
        }
    }

    @Test
    void updateProduct_ShouldThrowProductNotFoundException_WhenNotFound() {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.updateProduct(PRODUCT_ID, requestDto);
        });
        verify(repository, times(1)).findById(PRODUCT_ID);
        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteProduct_WhenExists() {
        when(repository.existsById(PRODUCT_ID)).thenReturn(true);
        doNothing().when(repository).deleteById(PRODUCT_ID);

        productService.deleteProduct(PRODUCT_ID);

        verify(repository, times(1)).existsById(PRODUCT_ID);
        verify(repository, times(1)).deleteById(PRODUCT_ID);
    }

    @Test
    void deleteProduct_ShouldThrowRuntimeException_WhenNotFound() {
        when(repository.existsById(PRODUCT_ID)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.deleteProduct(PRODUCT_ID);
        });
        assertEquals("Producto no encontrado", exception.getMessage());
        verify(repository, times(1)).existsById(PRODUCT_ID);
        verify(repository, never()).deleteById(PRODUCT_ID);
    }
}
