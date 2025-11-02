package com.plataforma_deliveri.payment_service.dtos;

public record PaymentResponseDto(
    String transactionId,
    Long orderId,
    String status,
    String message
) {}
