package com.plataforma_deliveri.order_service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.plataforma_deliveri.order_service.clients.ICatalogServiceFeignClient;
import com.plataforma_deliveri.order_service.dtos.OrderItemRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderItemResponseDto;
import com.plataforma_deliveri.order_service.dtos.OrderRequestDto;
import com.plataforma_deliveri.order_service.dtos.OrderResponseDto;
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
    private OrderMapper orderMapper;

    public List<OrderResponseDto> findAll() {
        return repository.findAll().stream()
            .map(orderMapper::toOrderResponseDTO)
            .collect(Collectors.toList());
    }

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
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product id: " + itemDto.id() + " no se ha encontrado");
            }

            if (productInfo.price() == null || productInfo.price() <= 0 || itemDto.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Precio o cantidad invalidos");
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

        Order savedOrder = repository.save(newOrder);
        
        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    
}
