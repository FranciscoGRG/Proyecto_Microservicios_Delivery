package com.plataforma_deliveri.payment_service.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plataforma_deliveri.payment_service.config.KafkaTopigConfig;
import com.plataforma_deliveri.payment_service.dtos.PaymentRequestDto;
import com.plataforma_deliveri.payment_service.dtos.PaymentResponseDto;
import com.plataforma_deliveri.payment_service.entities.PaymentTransaction;
import com.plataforma_deliveri.payment_service.repositories.IPaymentTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;

@Service
public class PaymentService {

    @Autowired
    private IPaymentTransactionRepository repository;

    @Autowired
    private KafkaTemplate<Long, String> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(@Value("${stripe.api.secretKey}") String stripeSecretKey) {
        Stripe.apiKey = stripeSecretKey;
    }

@Transactional
public PaymentResponseDto processPayment(PaymentRequestDto request) {
    try {
        Long amountInCents = Math.round(request.amount() * 100);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("order_id", request.orderId().toString());

        // 💳 Datos del token de la tarjeta
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("token", request.paymentMethodToken());

        Map<String, Object> paymentMethodData = new HashMap<>();
        paymentMethodData.put("type", "card");
        paymentMethodData.put("card", cardData);

        // 🚫 Desactivamos redirecciones automáticas
        Map<String, Object> automaticPaymentMethods = new HashMap<>();
        automaticPaymentMethods.put("enabled", true);
        automaticPaymentMethods.put("allow_redirects", "never");

        // 🔧 Parámetros del PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amountInCents);
        params.put("currency", request.currency());
        params.put("confirm", true);
        params.put("capture_method", "automatic");
        params.put("metadata", metadata);
        params.put("payment_method_data", paymentMethodData);
        params.put("automatic_payment_methods", automaticPaymentMethods); // ✅ agregado

        PaymentIntent intent = PaymentIntent.create(params);

        // Guardamos la transacción
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
                "Payment Intent creado y confirmado correctamente con ID: " + intent.getId());
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
                "Error al crear el intent: " + e.getMessage());
    }
}

    @Transactional
    public void handleWebhookEvent(String eventType, StripeObject dataObject) {

        PaymentIntent intent = null;
        try {
            if (dataObject instanceof PaymentIntent) {
                intent = (PaymentIntent) dataObject;
            } else {
                logger.warn("El objeto de datos no es PaymentIntent para el evento: {}", eventType);
                return;
            }
        } catch (Exception e) {
            logger.error("Error al castear StripeObject a PaymentIntent para evento {}: {}", eventType, e.getMessage());
            return;
        }

        final String stripeId = intent.getId();

        final String errorMessage = (intent.getLastPaymentError() != null
                && intent.getLastPaymentError().getMessage() != null)
                        ? intent.getLastPaymentError().getMessage()
                        : "Error de pago desconocido";

        if ("payment_intent.succeeded".equals(eventType)) {
            final String status = "SUCCEEDED";

            repository.findByStripePaymentId(stripeId).ifPresent(transaction -> {
                transaction.setStatus(status);
                repository.save(transaction);

                Long orderId = transaction.getOrderId();

                kafkaTemplate.send(KafkaTopigConfig.PAYMENT_EVENTS_TOPIC, orderId, status);

                logger.info("Pago exitoso registrado. Enviando notificación al Order-service con Order ID: {}",
                        orderId);
            });

        } else if ("payment_intent.payment_failed".equals(eventType)) {
            final String status = "FAILED";

            repository.findByStripePaymentId(stripeId).ifPresent(transaction -> {
                transaction.setStatus(status);
                transaction.setErrorMessage("Pago fallido: " + errorMessage);
                repository.save(transaction);

                Long orderId = transaction.getOrderId();

                kafkaTemplate.send(KafkaTopigConfig.PAYMENT_EVENTS_TOPIC, orderId, status);

                logger.warn("Pago fallido registrado para el Order ID: {}", orderId);
            });

        } else {
            logger.debug("Evento de Stripe recibido pero no manejado: {}", eventType);
        }
    }
}
