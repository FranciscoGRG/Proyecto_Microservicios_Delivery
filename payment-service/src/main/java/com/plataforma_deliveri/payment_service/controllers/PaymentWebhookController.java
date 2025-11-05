package com.plataforma_deliveri.payment_service.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.plataforma_deliveri.payment_service.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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
        StripeObject dataObject = null;
        String eventType = "UNKNOWN";

        try {
            // 1. Usar JsonParser para parsear el payload JSON completo
            JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();

            // 2. Extraer el tipo de evento y el objeto 'data'
            eventType = jsonPayload.get("type").getAsString();
            JsonObject data = jsonPayload.getAsJsonObject("data");
            JsonObject objectData = data.getAsJsonObject("object");

            // 3. Deserializar el objeto 'Event' (solo para obtener el tipo, etc.)
            Gson gson = new Gson();
            event = gson.fromJson(payload, Event.class); 

            // 4. Forzar la deserialización del objeto anidado a PaymentIntent usando el JSON crudo
            dataObject = PaymentIntent.GSON.fromJson(objectData, PaymentIntent.class);

        } catch (Exception e) {
            // Capturamos cualquier error de parsing (incluyendo si el JSON no tiene 'data' o 'object')
            logger.error("Error al parsear el Webhook JSON simulado para {}: {}", eventType, e.getMessage());
            return ResponseEntity.ok().build(); 
        }

        if (dataObject == null) {
            logger.warn("Objeto de datos nulo para el evento: {}", eventType);
            return ResponseEntity.ok().build();
        }

        logger.info("WebHook recibido, del tipo: {}", eventType); 

        // 💡 Si llegamos aquí, dataObject NO es nulo y activamos Kafka
        service.handleWebhookEvent(eventType, dataObject);

        return ResponseEntity.ok().build();
    }
}


