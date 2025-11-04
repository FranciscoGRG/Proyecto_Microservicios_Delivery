package com.plataforma_deliveri.order_service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plataforma_deliveri.order_service.clients.ICatalogServiceFeignClient;
import com.plataforma_deliveri.order_service.clients.IPaymentServiceFeignClient;
import com.plataforma_deliveri.order_service.consumers.PaymentEventsConsumer;
import com.plataforma_deliveri.order_service.dtos.OrderItemRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
import com.plataforma_deliveri.order_service.dtos.PaymentRequestDto;
import com.plataforma_deliveri.order_service.dtos.PaymentResponseDto;
import com.plataforma_deliveri.order_service.dtos.ProductDto;
import com.plataforma_deliveri.order_service.mappers.OrderMapper;
import com.plataforma_deliveri.order_service.models.Order;
import com.plataforma_deliveri.order_service.models.OrderItem;
import com.plataforma_deliveri.order_service.repositories.IOrderRepository;

@Service
public class OrderService {

    @Autowired
    private IOrderRepository repository;

    @Autowired
    private ICatalogServiceFeignClient catalogClient;

    @Autowired
    private IPaymentServiceFeignClient paymentClient;

    @Autowired
    private OrderMapper orderMapper;

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    public List<OrderResponseDto> findAll() {
        return repository.findAll().stream()
                .map(orderMapper::toOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request, String userEmail) {
        Order newOrder = new Order();
        newOrder.setUserEmail(userEmail);

        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;

        for (OrderItemRequestDto itemDto : request.items()) {
            ProductDto productInfo;
            try {
                productInfo = catalogClient.getProductById(itemDto.id());
            } catch (Exception e) {
                // Manejo de errores de comunicación o producto no encontrado
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en catálogo.");
            }

            OrderItem item = new OrderItem();
            item.setProductId(itemDto.id());
            item.setQuantity(itemDto.quantity());
            item.setPriceAtOrder(productInfo.price());
            item.setOrder(newOrder);
            items.add(item);
            total += productInfo.price() * itemDto.quantity();
        }

        newOrder.setItems(items);
        newOrder.setTotalPrice(total);
        newOrder.setStatus("PENDING_PAYMENT");

        Order savedOrder = repository.save(newOrder);

        PaymentRequestDto paymentDetails = new PaymentRequestDto(
                savedOrder.getId(),
                savedOrder.getTotalPrice(),
                "EUR",
                request.paymentMethodToken());

        try {
            PaymentResponseDto paymentResponse = paymentClient.processPayment(paymentDetails);

            if ("FAILED".equals(paymentResponse.status())) {
                savedOrder.setStatus("PAYMENT_FAILED_SYNC");
                repository.save(savedOrder);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago ha sido rechazado inmediatamente.");
            } else {
                savedOrder.setStatus("PAYMENT_INTENT_CREATED");
                repository.save(savedOrder);
            }

        } catch (ResponseStatusException rse) {
            savedOrder.setStatus("PAYMENT_ERROR_CLIENT");
            repository.save(savedOrder);
            throw rse;

        } catch (Exception e) {
            savedOrder.setStatus("PAYMENT_ERROR_COMMUNICATION");
            repository.save(savedOrder);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comunicarse con el servicio de pago: " + e.getMessage());
        }

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    public OrderResponseDto findById(Long id) {
        return orderMapper.toOrderResponseDTO(repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden con id: " + id + " no encontrada")));
    }

    public void deleteById(Long id) {
        Order orderToDelete = repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden con id: " + id + " no encontrada"));

        repository.delete(orderToDelete);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, String newStatus) {
        Order orderToUpdate = repository.findById(orderId).orElseThrow(() -> {
            logger.error("No se encontro la orden con id: {}", orderId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden con id: " + orderId + " no encontrada");
        });

        orderToUpdate.setStatus(newStatus);
        Order orderUpdated = repository.save(orderToUpdate);

        logger.info("Estado de la orden {} actualizado a {}", orderId, newStatus);

        if ("PAYMENT_FAILED".equalsIgnoreCase(newStatus)) {
            logger.warn("El pago fallo para la orden {}", orderId);
        }

        return orderMapper.toOrderResponseDTO(orderUpdated);
    }

}
