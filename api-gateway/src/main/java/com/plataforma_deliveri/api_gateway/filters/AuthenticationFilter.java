package com.plataforma_deliveri.api_gateway.filters;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login");

    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getURI().getPath();

            if (OPEN_API_ENDPOINTS.stream().anyMatch(requestPath::startsWith)) {
                return chain.filter(exchange);
            }

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return this.onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            Mono<String> validationMono = webClientBuilder.build()
                    .get()
                    .uri("http://users-service/api/v1/security/validate-token")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new WebClientResponseException(
                            response.statusCode().value(),
                            "Token validation failed in Users-Service",
                            response.headers().asHttpHeaders(),
                            null,
                            null)))
                    .onStatus(HttpStatusCode::is5xxServerError,
                            response -> Mono.error(new Exception("Users-Service Internal Error")))
                    .bodyToMono(String.class);

            return validationMono.flatMap(userEmail -> {
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Email", userEmail))
                        .build();

                return chain.filter(mutatedExchange);
            })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        return this.onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
                    })
                    .onErrorResume(e -> e.getMessage().contains("Users-Service Internal Error"), e -> {
                        return this.onError(exchange, "Users Service internal error", HttpStatus.INTERNAL_SERVER_ERROR);
                    })
                    .onErrorResume(Exception.class, e -> {
                        return this.onError(exchange, "Internal Service Error: Cannot connect to Users-Service",
                                HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("X-Error-Reason", error);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
