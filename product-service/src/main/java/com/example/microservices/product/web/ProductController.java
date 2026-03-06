package com.example.microservices.product.web;

import com.example.microservices.product.web.dto.ProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private static final Map<String, ProductDto> PRODUCTS = Map.of(
            "1", new ProductDto("1", "Laptop Pro", 1500.0),
            "2", new ProductDto("2", "Noise Cancelling Headphones", 300.0),
            "3", new ProductDto("3", "Mechanical Keyboard", 120.0)
    );

    @GetMapping("/{id}")
    @Cacheable("products")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProduct")
    @Retry(name = "productService")
    @RateLimiter(name = "productService")
    public ResponseEntity<ProductDto> getProduct(@PathVariable String id) {
        Instant start = Instant.now();
        log.info("Fetching product {} from 'backend' at {}", id, start);

        // Simulate a slow backend call
        maybeSlowCall();

        ProductDto product = PRODUCTS.get(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Returning product {} after {} ms", id, Duration.between(start, Instant.now()).toMillis());
        return ResponseEntity.ok(product);
    }

    private void maybeSlowCall() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ResponseEntity<ProductDto> fallbackProduct(String id, Throwable throwable) {
        log.warn("Fallback for product {} due to: {}", id, throwable.toString());
        ProductDto fallback = new ProductDto(id, "Fallback product", 0.0);
        return ResponseEntity.ok(fallback);
    }
}

