package dtos;

public record OrderItemResponseDto(
    String productId,
    Integer quantity,
    Double priceAtOrder,
    Double subtotal
) {}
