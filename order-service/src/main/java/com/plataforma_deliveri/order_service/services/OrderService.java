package com.plataforma_deliveri.order_service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plataforma_deliveri.order_service.clients.ICatalogServiceFeignClient;
import com.plataforma_deliveri.order_service.clients.IPaymentServiceFeignClient;
import com.plataforma_deliveri.order_service.dtos.OrderItemRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
import com.plataforma_deliveri.order_service.dtos.OrderStatusUpdateDto;
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

    // --- 1. Lógica de Consulta y Validación de Catálogo (Tu Código Actual) ---
    for (OrderItemRequestDto itemDto : request.items()) {
        ProductDto productInfo;
        // ... (Tu código para obtener productInfo y calcular total) ...
        try {
             productInfo = catalogClient.getProductById(itemDto.id());
        } catch (Exception e) {
             // Manejo de errores de comunicación o producto no encontrado
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en catálogo.");
        }
        
        // Asumiendo que el item se crea correctamente
        OrderItem item = new OrderItem();
        item.setProductId(itemDto.id());
        item.setQuantity(itemDto.quantity());
        item.setPriceAtOrder(productInfo.price());
        item.setOrder(newOrder);
        items.add(item);
        total += productInfo.price() * itemDto.quantity();
    }
    
    // --- 2. Preparar la Orden ---
    newOrder.setItems(items);
    newOrder.setTotalPrice(total);
    // 💡 Establecer estado inicial: La orden existe, pero el pago está pendiente.
    newOrder.setStatus("PENDING_PAYMENT"); 

    // Guardar la orden inicial para obtener el ID antes de llamar al pago
    Order savedOrder = repository.save(newOrder); 
    
    // --- 3. 💡 LLAMADA AL SERVICIO DE PAGO (NUEVO CÓDIGO) ---
    
    // Asumimos que el OrderRequestDto incluye un campo para el token de pago 
    // (o asume el token se gestiona en otro lugar, pero aquí lo enviamos)
    PaymentRequestDto paymentDetails = new PaymentRequestDto(
        savedOrder.getId(),
        savedOrder.getTotalPrice(),
        "EUR", // Moneda de tu sistema
        request.paymentMethodToken() // Debes añadir este campo al OrderRequestDto
    );

    try {
        PaymentResponseDto paymentResponse = paymentClient.processPayment(paymentDetails);
        
        // El PaymentService devuelve el estado del PaymentIntent (ej. PENDING, SUCCEEDED, FAILED)
        
        // 💡 Manejo de la Respuesta Sincrónica
        if ("FAILED".equals(paymentResponse.status())) {
             // Fallo inmediato (ej. datos de tarjeta inválidos)
             savedOrder.setStatus("PAYMENT_FAILED_SYNC");
             repository.save(savedOrder); // Guardar el estado de fallo
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago ha sido rechazado inmediatamente.");
        } else {
             // Pago iniciado/en proceso. La confirmación final vendrá por Webhook/Kafka.
             // El estado final de la orden será "PAID" cuando el webhook notifique el éxito.
             savedOrder.setStatus("PAYMENT_INTENT_CREATED"); 
             repository.save(savedOrder);
        }

    } catch (ResponseStatusException rse) {
        // Manejar errores HTTP específicos del Feign Client (ej. 404 del payment service)
        savedOrder.setStatus("PAYMENT_ERROR_CLIENT"); 
        repository.save(savedOrder);
        throw rse;
        
    } catch (Exception e) {
        // Manejar fallos de conexión (503 Service Unavailable)
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

    public OrderResponseDto updateOrder(Long id, OrderStatusUpdateDto request) {
        Order orderToUpdate = repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden con id: " + id + " no encontrada"));

        orderToUpdate.setStatus(request.status());

        Order updatedOrder = repository.save(orderToUpdate);

        return orderMapper.toOrderResponseDTO(updatedOrder);
    }

}
