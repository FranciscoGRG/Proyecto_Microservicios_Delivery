package com.plataforma_deliveri.catalog_service.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plataforma_deliveri.catalog_service.dtos.ProductRequestDto;
import com.plataforma_deliveri.catalog_service.dtos.ProductResponseDto;
import com.plataforma_deliveri.catalog_service.services.ProductService;


@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping()
    public ResponseEntity<List<ProductResponseDto>> findAll() {
        return ResponseEntity.ok(service.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping()
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductRequestDto product) {
        ProductResponseDto createdProduct = service.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable String id, @RequestBody ProductRequestDto product) {
        return ResponseEntity.ok(service.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
