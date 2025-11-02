package com.plataforma_deliveri.order_service.dtos;

public record PaymentRequestDto(
    Long orderId,
    Double amount,
    String currency, 
    String paymentMethodToken 
) {}
