package com.plataforma_deliveri.catalog_service.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.errors.ProductNotFoundException;
import com.plataforma_deliveri.catalog_service.models.Product;
import com.plataforma_deliveri.catalog_service.repositories.IProductRepository;

@Service
public class ProductService {

    @Autowired
    private IProductRepository repository;

    public ProductResponseDto createProduct(ProductRequestDto request) {
        Product product = ProductMapper.toEntity(request);

        Product productSaved = repository.save(product);

        return ProductMapper.toResponseDTO(productSaved);
    }

    public ProductResponseDto findById(String id) {
        Product product = repository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return ProductMapper.toResponseDTO(product);
    }

    public List<ProductResponseDto> getAllProducts() {
        List<Product> products = repository.findAll();

        return products.stream()
        .map(ProductMapper::toResponseDTO)
        .collect(Collectors.toList());
    }

    public ProductResponseDto updateProduct(String id, ProductRequestDto request) {
        Product existingProduct = repository.findById(id).orElseThrow(() -> new ProductNotFoundException("Producto con id: " + id + " no encontrado"));

        existingProduct.setName(request.name());
        existingProduct.setDescripction(request.description());
        existingProduct.setPrice(request.price());
        existingProduct.setStock(request.stock());
        existingProduct.setCategory(request.category());

        Product updatedProduct = repository.save(existingProduct);

        return ProductMapper.toResponseDTO(updatedProduct);
    }

    public void deleteProduct(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado");
        }

        repository.deleteById(id);
    }
}
