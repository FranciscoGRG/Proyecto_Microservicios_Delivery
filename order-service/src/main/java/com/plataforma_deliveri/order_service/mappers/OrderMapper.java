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
        
        String productId = item.getProductId();
        Integer quantity = item.getQuantity(); 
        Double priceAtOrder = item.getPriceAtOrder();

        Double subtotal = priceAtOrder * quantity;

        return new OrderItemResponseDto(
            productId, 
            quantity, 
            priceAtOrder, 
            subtotal
        );
    }

    public OrderResponseDto toOrderResponseDTO(Order order) {
        
        List<OrderItemResponseDto> itemDTOs = order.getItems().stream()
                .map(this::toOrderItemResponseDTO)
                .collect(Collectors.toList());

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
