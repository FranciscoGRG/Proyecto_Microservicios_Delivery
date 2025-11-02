package com.plataforma_deliveri.order_service.dtos;

public record PaymentResponseDto(
    String transactionId,
    Long orderId,
    String status,        
    String message
) {}
