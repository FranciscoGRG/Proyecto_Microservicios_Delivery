package com.plataforma_deliveri.api_gateway.filters;

import java.util.List;

import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpStatusCode;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    // Se mantiene el mismo listado para endpoints abiertos
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/travels"); // Asumimos que /travels es una ruta abierta temporalmente

    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        System.out.println("✅ [AuthFilter] Filtro de autenticación cargado.");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getURI().getPath();

            // 1. Check if the path is whitelisted (no token required)
            if (OPEN_API_ENDPOINTS.stream().anyMatch(requestPath::startsWith)) {
                System.out.println("➡️ [AuthFilter] Path abierto: " + requestPath);
                return chain.filter(exchange);
            }

            // 2. Check if the Authorization header is present
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                System.out.println("❌ [AuthFilter] ERROR: Authorization header is missing for path " + requestPath);
                return this.onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            System.out.println("🔍 [AuthFilter] Header encontrado. Iniciando validación para: " + requestPath);

            // 3. Delegate token validation to Users-Service
            Mono<String> validationMono = webClientBuilder.build()
                    .get()
                    // CRÍTICO: Asegúrate de que users-service está registrado en Eureka
                    .uri("http://users-service/api/v1/security/validate-token")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    
                    // Manejo de errores 4xx (Token inválido, etc.)
                    .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new WebClientResponseException(
                            response.statusCode().value(),
                            "Token validation failed in Users-Service",
                            response.headers().asHttpHeaders(),
                            null,
                            null)))
                    
                    // Manejo de errores 5xx (Servicio caído, error interno)
                    .onStatus(HttpStatusCode::is5xxServerError,
                            response -> Mono.error(new Exception("Users-Service Internal Error")))
                            
                    .bodyToMono(String.class);

            // 4. On successful validation, continue chain, adding the validated user email
            return validationMono.flatMap(userEmail -> {
                System.out.println("✅ [AuthFilter] Token Válido. User: " + userEmail);
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Email", userEmail))
                        .build();

                return chain.filter(mutatedExchange);
            })
            // 5. Error handling and resume
            .onErrorResume(WebClientResponseException.class, e -> {
                System.out.println("❌ [AuthFilter] Validación fallida por Users-Service: " + e.getMessage());
                return this.onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            })
            .onErrorResume(e -> e.getMessage().contains("Users-Service Internal Error"), e -> {
                System.out.println("❌ [AuthFilter] ERROR 500: Users-Service Internal Error.");
                return this.onError(exchange, "Users Service internal error", HttpStatus.INTERNAL_SERVER_ERROR);
            })
            .onErrorResume(Exception.class, e -> {
                System.out.println("❌ [AuthFilter] ERROR: No se puede conectar a Users-Service. " + e.getMessage());
                return this.onError(exchange, "Internal Service Error: Cannot connect to Users-Service",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        // Agregamos el error en un header para depuración
        exchange.getResponse().getHeaders().add("X-Error-Reason", error); 
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
