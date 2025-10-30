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
            } catch (RuntimeException e) { // 💡 Captura la RuntimeException del Decoder

                if ("PRODUCT_NOT_FOUND_IN_CATALOG".equals(e.getMessage())) {
                    // 🛑 Si el mensaje coincide, lanza tu mensaje personalizado con el ID
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Producto con id: " + itemDto.id() + " no se ha encontrado");
                }

                // Si es otra RuntimeException, relánzala
                throw e;

            } catch (Exception e) {
                // Puedes dejar este catch para otros errores de Feign que no sean 404 (ej.
                // problemas de conexión)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al comunicarse con el servicio de catálogo.");
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
