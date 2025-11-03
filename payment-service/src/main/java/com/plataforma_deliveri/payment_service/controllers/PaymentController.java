package com.plataforma_deliveri.payment_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plataforma_deliveri.payment_service.dtos.PaymentRequestDto;
import com.plataforma_deliveri.payment_service.dtos.PaymentResponseDto;
import com.plataforma_deliveri.payment_service.services.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService service;

    @PostMapping("/process")
    public PaymentResponseDto processPayment(@RequestBody PaymentRequestDto request) {
        return service.processPayment(request);
    }

    @GetMapping()
    public String getMethodName() {
        return new String("hola");
    }
    
    
}
