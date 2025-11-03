package com.plataforma_deliveri.payment_service.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plataforma_deliveri.payment_service.controllers.PaymentWebhookController;
import com.plataforma_deliveri.payment_service.dtos.PaymentRequestDto;
import com.plataforma_deliveri.payment_service.dtos.PaymentResponseDto;
import com.plataforma_deliveri.payment_service.entities.PaymentTransaction;
import com.plataforma_deliveri.payment_service.repositories.IPaymentTransactionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class PaymentService {

    @Autowired
    private IPaymentTransactionRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        try {
            Long amountInCents = Math.round(request.amount() * 100);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", request.orderId().toString());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currency())
                    .putAllMetadata(metadata)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setOrderId(request.orderId());
            transaction.setAmount(request.amount());
            transaction.setCurrency(request.currency());
            transaction.setStripePaymentId(intent.getId());
            transaction.setStatus(intent.getStatus());

            PaymentTransaction savedTransaction = repository.save(transaction);

            return new PaymentResponseDto(
                    savedTransaction.getStripePaymentId(),
                    savedTransaction.getOrderId(),
                    savedTransaction.getStatus().toUpperCase(),
                    "Payment Intent creado correctamente y con ID de stripe: " + intent.getId());
        } catch (StripeException e) {
            PaymentTransaction failedTransaction = new PaymentTransaction();
            failedTransaction.setOrderId(request.orderId());
            failedTransaction.setAmount(request.amount());
            failedTransaction.setCurrency(request.currency());
            failedTransaction.setStatus("FAILED");
            failedTransaction.setErrorMessage(e.getMessage());
            repository.save(failedTransaction);

            return new PaymentResponseDto(
                null,
                request.orderId(),
                "FAILED",
                "Error al crear el intent: " + e.getMessage()
            );
        }
    }

    @Transactional
    public void handleWebhookEvent(String eventType, StripeObject dataObject) {
        if ("payment_intent.succeeded".equals(eventType)) {
            PaymentIntent intent = (PaymentIntent) dataObject;
            String stripeId = intent.getId();

            repository.findByStripePaymentId(stripeId).ifPresent(transaction -> {
                transaction.setStatus("SUCCEEDED");
                repository.save(transaction);

                logger.info("Pago existoso registrado. Enviando notificacion al Order-service con el id: " + transaction.getId());
            });
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            PaymentIntent intent = (PaymentIntent) dataObject;
            String stripeId = intent.getId();

            repository.findByStripePaymentId(stripeId).ifPresent(transaction -> {
                transaction.setStatus("FAILED");
                transaction.setErrorMessage("Pago fallido: " + intent.getLastPaymentError().getMessage());
                repository.save(transaction);

                logger.warn("Pago fallido registrado para el orderID: " + transaction.getId());
            });
        }
    }
}
