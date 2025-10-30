package com.fran.apigateway.app.filters;

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

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login"
    );

    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (OPEN_API_ENDPOINTS.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authorization header missing or malformed", HttpStatus.UNAUTHORIZED);
            }

            return webClientBuilder.build()
                    .get()
                    .uri("http://users-service/api/v1/security/validate-token")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> 
                        Mono.error(new WebClientResponseException(
                            response.statusCode().value(), 
                            "Token validation failed in Users-Service", 
                            response.headers().asHttpHeaders(), null, null
                        ))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response -> 
                        Mono.error(new Exception("Users-Service Internal Error"))
                    )
                    .bodyToMono(String.class)
                    .flatMap(userEmail -> {
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(r -> r.headers(headers -> headers.add("X-User-Email", userEmail)))
                                .build();

                        return chain.filter(mutatedExchange);
                    })
                    .onErrorResume(WebClientResponseException.class, e -> 
                        onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED)
                    )
                    .onErrorResume(e -> e.getMessage() != null && e.getMessage().contains("Users-Service Internal Error"),
                        e -> onError(exchange, "Users Service internal error", HttpStatus.INTERNAL_SERVER_ERROR))
                    .onErrorResume(Exception.class, e -> 
                        onError(exchange, "Internal Service Error: Cannot connect to Users-Service", HttpStatus.INTERNAL_SERVER_ERROR)
                    );
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("X-Error-Reason", error);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}