package com.plataforma_deliveri.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.plataforma_deliveri.order_service.errors.CustomErrorDecoder;

@Configuration
public class FeignConfig {
@Bean
    public CustomErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}
