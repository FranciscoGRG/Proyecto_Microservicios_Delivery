package com.plataforma_deliveri.payment_service.controllers;

import com.plataforma_deliveri.payment_service.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("❌ Error en la verificación de la firma del webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma no válida");
        } catch (Exception e) {
            logger.error("❌ Error al construir el evento de Stripe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Evento inválido");
        }

        String eventType = event.getType();
        StripeObject dataObject = null;

        try {
            // ✅ Deserialización manual del objeto data.object
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            switch (eventType) {
                case "payment_intent.succeeded":
                case "payment_intent.payment_failed":
                case "payment_intent.created":
                    dataObject = ApiResource.GSON.fromJson(rawJson, PaymentIntent.class);
                    break;
                case "charge.succeeded":
                    dataObject = ApiResource.GSON.fromJson(rawJson, Charge.class);
                    break;
                default:
                    logger.debug("Evento Stripe no manejado explícitamente: {}", eventType);
                    break;
            }

            if (dataObject == null) {
                logger.warn("⚠️ Objeto de datos nulo para el evento: {}", eventType);
                return ResponseEntity.ok("Evento recibido sin objeto");
            }

            logger.info("✅ Webhook recibido correctamente: {}", eventType);
            service.handleWebhookEvent(eventType, dataObject);

            return ResponseEntity.ok("Evento procesado correctamente");

        } catch (Exception e) {
            logger.error("❌ Error procesando evento {}: {}", eventType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno");
        }
    }
}
