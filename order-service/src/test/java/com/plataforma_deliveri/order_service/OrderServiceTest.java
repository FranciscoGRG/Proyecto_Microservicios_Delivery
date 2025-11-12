package com.plataforma_deliveri.order_service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.plataforma_deliveri.order_service.clients.ICatalogServiceFeignClient;
import com.plataforma_deliveri.order_service.clients.IPaymentServiceFeignClient;
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
import com.plataforma_deliveri.order_service.services.OrderService;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private IOrderRepository repository;

    @Mock
    private ICatalogServiceFeignClient catalogClient;

    @Mock
    private IPaymentServiceFeignClient paymentClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private final Long ORDER_ID = 1L;
    private final String USER_EMAIL = "test@example.com";
    private final String PRODUCT_ID = "P001";
    private final double PRODUCT_PRICE = 10.0;

    private Order order;
    private OrderResponseDto orderResponseDto;
    private OrderRequestDto orderRequestDto;
    private ProductDto productDto;
    private PaymentResponseDto paymentSuccessResponse;
    private PaymentResponseDto paymentFailedResponse;

    @BeforeEach
    void setUp() {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(PRODUCT_ID);
        orderItem.setQuantity(2);
        orderItem.setPriceAtOrder(PRODUCT_PRICE);

        order = new Order();
        order.setId(ORDER_ID);
        order.setUserEmail(USER_EMAIL);
        order.setItems(Collections.singletonList(orderItem));
        order.setTotalPrice(PRODUCT_PRICE * 2);
        order.setStatus("PAYMENT_INTENT_CREATED");

        orderResponseDto = new OrderResponseDto(
                ORDER_ID,
                USER_EMAIL,
                "PAYMENT_INTENT_CREATED",
                (PRODUCT_PRICE * 2),
                LocalDateTime.now(),
                Collections.emptyList());

        OrderItemRequestDto itemRequestDto = new OrderItemRequestDto(PRODUCT_ID, 2);
        orderRequestDto = new OrderRequestDto(
                Collections.singletonList(itemRequestDto),
                "token_payment_123");

        productDto = new ProductDto(PRODUCT_ID, "Test Product", "Description", PRODUCT_PRICE, 100, "Category");

        paymentSuccessResponse = new PaymentResponseDto(
            "ref_success_123",
            ORDER_ID,
            "SUCCESS",
            "Pago procesado correctamente");
            
        paymentFailedResponse = new PaymentResponseDto(
            null,
            ORDER_ID,
            "FAILED",
            "Fondos insuficientes");
    }

    @Test
    void findAll_ShouldReturnListOfOrders() {
        when(repository.findAll()).thenReturn(Collections.singletonList(order));
        when(orderMapper.toOrderResponseDTO(any(Order.class))).thenReturn(orderResponseDto);

        List<OrderResponseDto> result = orderService.findAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void findById_ShouldReturnOrderResponseDto_WhenFound() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderResponseDTO(any(Order.class))).thenReturn(orderResponseDto);

        OrderResponseDto result = orderService.findById(ORDER_ID);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.id());
        verify(repository, times(1)).findById(ORDER_ID);
    }

    @Test
    void findById_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.findById(99L);
        });

        assertEquals("404 NOT_FOUND", exception.getStatusCode().toString());
        verify(repository, times(1)).findById(99L);
    }

    @Test
    void deleteById_ShouldCallRepositoryDelete_WhenFound() {
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        doNothing().when(repository).delete(any(Order.class));

        assertDoesNotThrow(() -> orderService.deleteById(ORDER_ID));

        verify(repository, times(1)).findById(ORDER_ID);
        verify(repository, times(1)).delete(order);
    }

    @Test
    void deleteById_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.deleteById(99L);
        });

        assertEquals("404 NOT_FOUND", exception.getStatusCode().toString());
        verify(repository, times(1)).findById(99L);
        verify(repository, never()).delete(any(Order.class));
    }

    @Test
    void createOrder_ShouldProcessSuccessfully_AndReturnPaymentIntentCreated() {
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDto);

        when(repository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = (Order) invocation.getArgument(0);
                    if (savedOrder.getStatus().equals("PENDING_PAYMENT")) {
                        savedOrder.setId(ORDER_ID);
                    }
                    return savedOrder;
                });

        when(paymentClient.processPayment(any(PaymentRequestDto.class))).thenReturn(paymentSuccessResponse);

        when(orderMapper.toOrderResponseDTO(any(Order.class))).thenReturn(orderResponseDto);

        OrderResponseDto result = orderService.createOrder(orderRequestDto, USER_EMAIL);

        assertNotNull(result);
        assertEquals("PAYMENT_INTENT_CREATED", result.status());
        assertEquals(PRODUCT_PRICE * 2, result.totalPrice());

        verify(catalogClient, times(1)).getProductById(PRODUCT_ID);
        verify(paymentClient, times(1)).processPayment(any(PaymentRequestDto.class));
        verify(repository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowInternalServerError_WhenCatalogClientFails() {
        when(catalogClient.getProductById(PRODUCT_ID)).thenThrow(new RuntimeException("Error de conexión"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.createOrder(orderRequestDto, USER_EMAIL);
        });

        assertEquals("500 INTERNAL_SERVER_ERROR", exception.getStatusCode().toString());
        assertTrue(exception.getReason().contains("Error en catálogo"));

        verify(catalogClient, times(1)).getProductById(PRODUCT_ID);
        verify(repository, never()).save(any(Order.class));
        verify(paymentClient, never()).processPayment(any(PaymentRequestDto.class));
    }

    @Test
    void createOrder_ShouldSetStatusToPaymentFailedSync_WhenPaymentFailsSync() {
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDto);
        when(repository.save(any(Order.class))).thenReturn(order);

        when(paymentClient.processPayment(any(PaymentRequestDto.class))).thenReturn(paymentFailedResponse);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.createOrder(orderRequestDto, USER_EMAIL);
        });

        assertEquals("400 BAD_REQUEST", exception.getStatusCode().toString());
        assertTrue(exception.getReason().contains("El pago ha sido rechazado inmediatamente"));

        verify(repository, times(3)).save(any(Order.class));
        verify(paymentClient, times(1)).processPayment(any(PaymentRequestDto.class));
    }

    @Test
    void createOrder_ShouldSetStatusToPaymentErrorCommunication_WhenPaymentClientThrowsException() {
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDto);
        when(repository.save(any(Order.class))).thenReturn(order);

        when(paymentClient.processPayment(any(PaymentRequestDto.class)))
                .thenThrow(new RuntimeException("Error HTTP 503"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.createOrder(orderRequestDto, USER_EMAIL);
        });

        assertEquals("500 INTERNAL_SERVER_ERROR", exception.getStatusCode().toString());
        assertTrue(exception.getReason().contains("Error al comunicarse con el servicio de pago"));

        verify(repository, times(2)).save(any(Order.class));
        verify(paymentClient, times(1)).processPayment(any(PaymentRequestDto.class));
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatusAndReturnUpdatedOrder() {
        String newStatus = "DELIVERED";
        Order updatedOrder = new Order();
        updatedOrder.setId(ORDER_ID);
        updatedOrder.setStatus(newStatus);

        OrderResponseDto updatedResponseDto = new OrderResponseDto(
                ORDER_ID,
                USER_EMAIL,
                newStatus,
                10.0,
                LocalDateTime.now(),
                Collections.emptyList());

        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toOrderResponseDTO(updatedOrder)).thenReturn(updatedResponseDto);

        OrderResponseDto result = orderService.updateOrderStatus(ORDER_ID, newStatus);

        assertNotNull(result);
        assertEquals(newStatus, result.status());
        verify(repository, times(1)).findById(ORDER_ID);
        verify(repository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldThrowNotFound_WhenOrderNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.updateOrderStatus(99L, "SHIPPED");
        });

        assertEquals("404 NOT_FOUND", exception.getStatusCode().toString());
        verify(repository, times(1)).findById(99L);
        verify(repository, never()).save(any(Order.class));
    }
}