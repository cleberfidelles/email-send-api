package com.fidellesdev.email_send.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiKeyFilterTest {

    private final ApiKeyFilter filter = new ApiKeyFilter("segredo");

    private MockHttpServletResponse chamar(String uri, String chave, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        if (chave != null) {
            request.addHeader(ApiKeyFilter.HEADER, chave);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    @DisplayName("chave correta: deixa a requisição passar")
    void chaveCorreta() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse response = chamar("/email", "segredo", chain);

        verify(chain).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("chave errada: 401")
    void chaveErrada() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse response = chamar("/email", "errada", chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("sem header: 401")
    void semHeader() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse response = chamar("/email/1", null, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("rotas fora de /email (ex.: Swagger) não exigem a chave")
    void rotaLivre() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        chamar("/swagger-ui/index.html", null, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("chave configurada em branco impede a aplicação de subir")
    void chaveEmBranco() {
        assertThrows(IllegalStateException.class, () -> new ApiKeyFilter("  "));
    }
}
