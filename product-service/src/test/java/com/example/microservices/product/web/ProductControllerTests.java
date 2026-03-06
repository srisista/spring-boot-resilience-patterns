package com.example.microservices.product.web;

import com.example.microservices.product.web.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductControllerTests {

    private final ProductController controller = new ProductController();

    @Test
    void getExistingProductReturnsOk() {
        ResponseEntity<ProductDto> response = controller.getProduct("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().id());
    }

    @Test
    void getMissingProductReturnsNotFound() {
        ResponseEntity<ProductDto> response = controller.getProduct("999");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

