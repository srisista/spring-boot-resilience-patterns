## Spring Boot Microservices Architecture – Production Patterns Demo

This repository is a **production-style Spring Boot microservices playground** that demonstrates the patterns you listed:

- **Auto configuration & starter dependencies**
- **Embedded servers**
- **External configuration (Config Server + `application.yml`)**
- **Spring Boot Actuator monitoring**
- **Resilience4j – circuit breaker, retry, rate limiting**
- **Service discovery (Eureka)**
- **API Gateway (Spring Cloud Gateway)**
- **Distributed tracing (Micrometer + Zipkin exporter)**
- **Caching**
- **Spring Security basics**
- **DevTools hot reload**
- **CI/CD with GitHub Actions**

The goal is: **anyone should be able to open this repo, see the structure, and quickly understand where and how each feature is implemented.**

---

### Skills / keywords this repo demonstrates

- Java 17, Spring Boot 3.x, Spring Framework
- Microservices architecture, RESTful REST APIs, API Gateway pattern
- Spring Cloud Netflix Eureka service discovery, Spring Cloud Gateway
- Spring Cloud Config Server, centralized configuration management
- Resilience4j (circuit breaker, retry, rate limiting), Spring Retry
- Spring Security (HTTP Basic, secured endpoints)
- Micrometer, distributed tracing, OpenTelemetry, Zipkin
- Caching, Redis-ready configuration
- Spring Boot Actuator, health checks, metrics, observability
- Docker, Docker Compose, containerized microservices
- JUnit 5, JaCoCo test coverage, GitHub Actions CI/CD
- Maven multi-module project structure, build automation

---

### Modules overview

- **`common-lib`**: Shared code (DTOs, constants, future utilities).
- **`config-server`**: Spring Cloud Config Server – central external configuration.
- **`discovery-service`**: Netflix Eureka server for service discovery.
- **`api-gateway`**: Spring Cloud Gateway – single entry point to backend services.
- **`product-service`**: Example business microservice demonstrating monitoring, resilience, caching, tracing and security.

---

### Low-level design (per service)

- **Config Server (`config-server`)**
  - **Port**: `8888`
  - **Main class**: `com.example.microservices.config.ConfigServerApplication`
  - **Responsibilities**:
    - Start Spring Cloud Config Server.
    - Serve externalized configuration (`application.yml`, ready for Git-backed config).
  - **Key config**:
    - `spring.profiles.active=native`
    - `spring.cloud.config.server.native.search-locations=classpath:/config`

- **Discovery Service / Eureka (`discovery-service`)**
  - **Port**: `8761`
  - **Main class**: `com.example.microservices.discovery.DiscoveryServiceApplication`
  - **Responsibilities**:
    - Run Eureka registry (`@EnableEurekaServer`).
    - Hold registry of `api-gateway`, `product-service`, etc.
  - **Key config**:
    - `eureka.client.register-with-eureka=false`
    - `eureka.client.fetch-registry=false` (standalone server).

- **API Gateway (`api-gateway`)**
  - **Port**: `8080`
  - **Main class**: `com.example.microservices.gateway.ApiGatewayApplication`
  - **Responsibilities**:
    - Single public entry point for clients.
    - Route `/products/**` to `product-service` via Eureka.
  - **Routing config** (`application.yml`):
    - Route ID: `product-service`.
    - Predicate: `Path=/products/**`.
    - URI: `lb://product-service` (load-balanced via Eureka).
    - Filter: `StripPrefix=1` so `/products/1` → `/1` on downstream.

- **Product Service (`product-service`)**
  - **Port**: `9001`
  - **Main class**: `com.example.microservices.product.ProductServiceApplication`
    - Annotations: `@SpringBootApplication`, `@EnableDiscoveryClient`, `@EnableCaching`, `@EnableRetry`.
  - **Package structure**:
    - `com.example.microservices.product.config`
      - `SecurityConfig`: HTTP Basic, secures `/products/**`, exposes `/actuator/**`.
    - `com.example.microservices.product.web`
      - `ProductController`: REST controller for `/products/{id}`.
    - `com.example.microservices.product.web.dto`
      - `ProductDto`: record with `id`, `name`, `price`.
  - **Endpoint behavior** (`ProductController#getProduct`):
    - Path: `GET /products/{id}`.
    - In-memory product map for demo (`PRODUCTS` map).
    - Simulates latency (`Thread.sleep(500)`).
    - Returns:
      - `200 OK` with `ProductDto` if found.
      - `404 NOT FOUND` if ID is unknown.
  - **Resilience & caching annotations** on `getProduct`:
    - `@Cacheable("products")` → First call hits backend, subsequent calls served from cache.
    - `@CircuitBreaker(name = "productService", fallbackMethod = "fallbackProduct")` → When failure rate exceeds threshold, calls go to fallback.
    - `@Retry(name = "productService")` → Transparently retries transient failures.
    - `@RateLimiter(name = "productService")` → Limits number of calls per second.
  - **Fallback path**:
    - `fallbackProduct(String id, Throwable throwable)` returns a safe `ProductDto` with `"Fallback product"` when the circuit is open or repeated failures occur.

---

### Request flow: GET `/products/{id}` (via gateway)

1. **Client** sends `GET http://localhost:8080/products/1`.
2. **API Gateway** (`api-gateway`) receives the request on port `8080`.
3. Gateway **matches route** with predicate `Path=/products/**` and forwards to `lb://product-service` (service name resolved via **Eureka**).
4. **Eureka** (`discovery-service`) returns the list of `product-service` instances (here, one container).
5. Gateway forwards the request to **Product Service** (`product-service` on port `9001`).
6. **Product Service**:
   - Checks the cache (`@Cacheable("products")`).
   - If not cached:
     - Executes `getProduct` logic.
     - Applies **Resilience4j** policies (rate limiter, retry, circuit breaker).
     - Populates the cache on success.
7. Response flows back:
   - `ProductDto` → Gateway → Client (`200 OK` or `404`).

All along the path:

- **Actuator** collects health and metrics.
- **Tracing** (Micrometer + OpenTelemetry) can export spans to Zipkin.
- **Security** (on `product-service`) ensures `/products/**` is not exposed anonymously (for non-gateway calls).

---

### Where to see each feature

- **Auto configuration & starter dependencies**
  - Look at the `pom.xml` files in each module (`spring-boot-starter-*`, `resilience4j-spring-boot3`, `spring-cloud-starter-*`).
- **Embedded servers**
  - Each `*Application` class (`ConfigServerApplication`, `DiscoveryServiceApplication`, `ApiGatewayApplication`, `ProductServiceApplication`) runs an embedded server via `SpringApplication.run(...)`.
- **External configuration**
  - `config-server/src/main/resources/application.yml` – config server setup.
  - Each service’s `application.yml` shows environment-style configuration.
- **Spring Boot Actuator**
  - Dependencies: `spring-boot-starter-actuator` in `config-server`, `discovery-service`, `api-gateway`, `product-service`.
  - Endpoints like `/actuator/health`, `/actuator/info`, `/actuator/metrics`.
- **Resilience4j (circuit breaker, retry, rate limiting)**
  - `product-service`:
    - `ProductController` methods annotated with `@CircuitBreaker`, `@Retry`, `@RateLimiter`.
    - Configuration in `product-service/src/main/resources/application.yml`.
- **Service discovery (Eureka)**
  - `discovery-service` with `@EnableEurekaServer`.
  - `api-gateway` and `product-service` as Eureka clients.
- **API Gateway**
  - `api-gateway`:
    - Routing config in `application.yml` (route to `lb://product-service` on `/products/**`).
- **Distributed tracing**
  - `product-service`:
    - Micrometer tracing dependencies (`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-zipkin`).
    - Tracing sampling configured under `management.tracing` in `application.yml`.
- **Caching**
  - `product-service`:
    - `@EnableCaching` on `ProductServiceApplication`.
    - `@Cacheable("products")` on the `getProduct` endpoint.
- **Security**
  - `product-service`:
    - `SecurityConfig` requiring auth for `/products/**`, exposing `/actuator/**`.
- **DevTools hot reload**
  - `spring-boot-devtools` dependency in `product-service/pom.xml` (runtime, optional).
- **CI/CD**
  - `.github/workflows/maven-build.yml` – GitHub Actions pipeline that builds and tests the whole multi-module project on each push/PR.

---

### How to run the system locally

1. **Build everything**
   - `mvn clean install`
2. **Start services in this order**
   - `config-server` (port `8888`)
   - `discovery-service` (port `8761`)
   - `product-service` (port `9001`)
   - `api-gateway` (port `8080`)
3. **Access through API Gateway**
   - Example: `GET http://localhost:8080/products/1`
   - Health: `GET http://localhost:8080/actuator/health`

You can now extend this repo with more services (order, inventory, etc.), more advanced security (JWT/OAuth2), and Kubernetes/Docker deployment manifests while keeping the current **clean, modular structure**.

---

### License

This project is licensed under the **MIT License**. See the `LICENSE` file for full details.
