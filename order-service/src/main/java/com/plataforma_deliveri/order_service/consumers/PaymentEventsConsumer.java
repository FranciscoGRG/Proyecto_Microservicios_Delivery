package com.plataforma_deliveri.order_service.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.plataforma_deliveri.order_service.services.OrderService;

@Component
public class PaymentEventsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    @Autowired
    private OrderService service;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(@Payload String status, @Header(KafkaHeaders.RECEIVED_KEY) Long orderId) {
        logger.info("Evento pagado recibido: Order ID: {}, Estado: {}", orderId, status);

        if ("SUCCEEDED".equalsIgnoreCase(status)) {
            service.updateOrderStatus(orderId, "COMPLETED");
        } else if ("FAILED".equalsIgnoreCase(status)) {
            service.updateOrderStatus(orderId, "PAYMENT_FAILED");
        }
    }
}
