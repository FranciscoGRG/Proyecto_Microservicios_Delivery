package com.plataforma_deliveri.payment_service.dtos;

public record PaymentRequestDto(
    Long orderId,
    Double amount,
    String currency,
    String paymentMethodToken
) {}
