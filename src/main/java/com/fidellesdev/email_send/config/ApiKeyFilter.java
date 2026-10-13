package com.fidellesdev.email_send.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Exige o header X-API-Key em /email/**. Evita que qualquer pessoa que descubra
 * a URL use a sua conta de e-mail para disparar mensagens.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final byte[] chaveEsperada;

    public ApiKeyFilter(@Value("${app.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A propriedade app.api-key (variável API_KEY) não pode ser vazia");
        }
        this.chaveEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/email");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String enviada = request.getHeader(HEADER);
        // comparação em tempo constante
        if (enviada != null && MessageDigest.isEqual(chaveEsperada, enviada.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"title\":\"Não autorizado\",\"status\":401,\"detail\":\"API key ausente ou inválida\"}");
    }
}
