package com.plataforma_deliveri.order_service.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.plataforma_deliveri.order_service.dtos.OrderItemResponseDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
import com.plataforma_deliveri.order_service.models.Order;
import com.plataforma_deliveri.order_service.models.OrderItem;

@Component
public class OrderMapper {
    public OrderItemResponseDto toOrderItemResponseDTO(OrderItem item) {
        
        // 1. Obtener los valores de la entidad
        String productId = item.getProductId();
        Integer quantity = item.getQuantity(); // Nota: Aquí SÍ usamos .getQuantity() porque OrderItem es una ENTIDAD JPA (no un Record)
        Double priceAtOrder = item.getPriceAtOrder(); // Y .getPriceAtOrder()

        // 2. Calcular el subtotal
        Double subtotal = priceAtOrder * quantity;

        // 3. Crear el Record usando el constructor generado por el compilador
        return new OrderItemResponseDto(
            productId, 
            quantity, 
            priceAtOrder, 
            subtotal
        );
    }

    /**
     * Mapea una entidad Order a su DTO de respuesta (Record).
     * Usa el constructor del Record.
     */
    public OrderResponseDto toOrderResponseDTO(Order order) {
        
        // Mapea la lista de ítems primero
        List<OrderItemResponseDto> itemDTOs = order.getItems().stream()
                .map(this::toOrderItemResponseDTO)
                .collect(Collectors.toList());

        // Crea el Record OrderResponseDTO usando el constructor
        return new OrderResponseDto(
            order.getId(),
            order.getUserEmail(),
            order.getStatus(),
            order.getTotalPrice(),
            order.getCreatedAt(),
            itemDTOs
        );
    }
}
