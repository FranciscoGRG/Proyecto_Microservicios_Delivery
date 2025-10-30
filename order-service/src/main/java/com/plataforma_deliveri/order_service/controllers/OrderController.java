package com.plataforma_deliveri.order_service.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plataforma_deliveri.order_service.dtos.OrderRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
import com.plataforma_deliveri.order_service.services.OrderService;

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
            @RequestBody OrderRequestDto request)
            {
                String userEmail = "fgrcalifa@gmail.com";
        OrderResponseDto newOrder = service.createOrder(request, userEmail);
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<List<OrderResponseDto>> findAll2() {
        return ResponseEntity.ok(service.findAll());
    }

}
