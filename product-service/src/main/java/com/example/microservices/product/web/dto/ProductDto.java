package com.example.microservices.product.web.dto;

public record ProductDto(
        String id,
        String name,
        double price
) {
}

