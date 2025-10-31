package com.plataforma_deliveri.order_service.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plataforma_deliveri.order_service.dtos.OrderRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
import com.plataforma_deliveri.order_service.dtos.OrderStatusUpdateDto;
import com.plataforma_deliveri.order_service.services.OrderService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService service;

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody OrderRequestDto request,
            @RequestHeader("X-User-Email") String userEmail) {
        // String userEmail = "fgrcalifa@gmail.com";
        OrderResponseDto newOrder = service.createOrder(request, userEmail);
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateDto request) {
        OrderResponseDto updatedOrder = service.updateOrder(id, request);
        return ResponseEntity.ok(updatedOrder);
    }

}
