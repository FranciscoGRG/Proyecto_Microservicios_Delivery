package com.plataforma_deliveri.payment_service.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plataforma_deliveri.payment_service.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Autowired
    private PaymentService service;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/events")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Error en la verificacion de la firma del webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Firma del Webhook no valida");
        } catch (Exception e) {
            logger.error("Error al construir el evento de Stripe: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Payload de Webhook invalido");
        }

        String eventType = event.getType();

        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (dataObject == null) {
            logger.warn("Objeto de datos nulo para el evento: {}", eventType);
            return ResponseEntity.ok().build();
        }

        logger.info("WebHook recibido, del tipo: {}", eventType);

        service.handleWebhookEvent(eventType, dataObject);

        return ResponseEntity.ok().build();
    }
}
