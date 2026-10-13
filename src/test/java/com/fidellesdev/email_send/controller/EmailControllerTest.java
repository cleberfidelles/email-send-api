package com.fidellesdev.email_send.controller;

import com.fidellesdev.email_send.dtos.EmailRequestDto;
import com.fidellesdev.email_send.dtos.EmailResponseDto;
import com.fidellesdev.email_send.enums.StatusEnvio;
import com.fidellesdev.email_send.exception.ApiExceptionHandler;
import com.fidellesdev.email_send.exception.EmailNaoEncontradoException;
import com.fidellesdev.email_send.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EmailController(emailService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static final String JSON_VALIDO = """
            {"assunto":"Olá","destinatario":"destino@teste.com","mensagem":"Corpo"}
            """;

    @Test
    @DisplayName("POST /email: 202, header Location e status PENDENTE")
    void enviar_deveRetornar202ComLocation() throws Exception {
        when(emailService.solicitarEnvio(any(EmailRequestDto.class))).thenReturn(
                new EmailResponseDto(1L, "Olá", "destino@teste.com", "Corpo",
                        StatusEnvio.PENDENTE, LocalDateTime.now(), null));

        mockMvc.perform(post("/email").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "http://localhost/email/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statusEnvio").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /email: 400 com os campos inválidos")
    void enviar_comDadosInvalidos_deveRetornar400() throws Exception {
        String invalido = """
                {"assunto":"","destinatario":"nao-e-email","mensagem":""}
                """;

        mockMvc.perform(post("/email").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.assunto").exists())
                .andExpect(jsonPath("$.campos.destinatario").exists())
                .andExpect(jsonPath("$.campos.mensagem").exists());

        verify(emailService, never()).solicitarEnvio(any());
    }

    @Test
    @DisplayName("POST /email: 400 quando o assunto tem quebra de linha")
    void enviar_comQuebraDeLinhaNoAssunto_deveRetornar400() throws Exception {
        String json = """
                {"assunto":"linha1\\nBcc: x@y.com","destinatario":"destino@teste.com","mensagem":"Corpo"}
                """;

        mockMvc.perform(post("/email").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.assunto").exists());
    }

    @Test
    @DisplayName("POST /email: 400 quando o corpo está ausente")
    void enviar_semCorpo_deveRetornar400() throws Exception {
        mockMvc.perform(post("/email").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /email/{id}: 200 com o status")
    void buscar_quandoExiste_deveRetornar200() throws Exception {
        when(emailService.buscarPorId(7L)).thenReturn(
                new EmailResponseDto(7L, "Olá", "destino@teste.com", "Corpo",
                        StatusEnvio.ENVIADO, LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/email/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.statusEnvio").value("ENVIADO"));
    }

    @Test
    @DisplayName("GET /email/{id}: 404 quando não existe")
    void buscar_quandoNaoExiste_deveRetornar404() throws Exception {
        when(emailService.buscarPorId(99L)).thenThrow(new EmailNaoEncontradoException(99L));

        mockMvc.perform(get("/email/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("E-mail não encontrado"));
    }
}
