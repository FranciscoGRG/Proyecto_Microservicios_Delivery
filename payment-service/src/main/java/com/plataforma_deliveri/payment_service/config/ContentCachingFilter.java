package com.plataforma_deliveri.payment_service.config;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@Order(1)
public class ContentCachingFilter implements Filter {
@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest currentRequest = (HttpServletRequest) request;
            
            // Si la URL contiene la palabra "webhooks", envuelve la solicitud
            if (currentRequest.getRequestURI().contains("/webhooks")) {
                 ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(currentRequest);
                 chain.doFilter(wrappedRequest, response);
                 return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
