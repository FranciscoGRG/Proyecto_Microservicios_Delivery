package com.plataforma_deliveri.order_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.plataforma_deliveri.order_service.dtos.PaymentRequestDto;
import com.plataforma_deliveri.order_service.dtos.PaymentResponseDto;

@FeignClient(name = "payment-service")
public interface IPaymentServiceFeignClient {
    @PostMapping("/api/v1/payments/process")
    PaymentResponseDto processPayment(PaymentRequestDto request);
}
