package com.fran.users_service.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fran.users_service.app.services.JwtService;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException, java.io.IOException {

        String path = request.getRequestURI();


        // CORRECCIÓN DEFINITIVA: Si es OPTIONS, Login/Register, o un GET a /travels, saltamos la validación.
        if (request.getMethod().equals(HttpMethod.OPTIONS.name()) ||
                path.endsWith("/api/v1/users/login") ||
                path.endsWith("/api/v1/users/register")) {

            // Permitimos que la cadena continúe, permitiendo que SecurityConfig aplique el 'permitAll()'.
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // Esta lógica maneja todas las peticiones restantes (POST, PUT, DELETE, y rutas autenticadas)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Si falta el token en una ruta que requiere autenticación,
            // permitimos que la cadena continúe para que Spring Security la bloquee con un 401/403.
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String email = jwtService.extractEmail(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}