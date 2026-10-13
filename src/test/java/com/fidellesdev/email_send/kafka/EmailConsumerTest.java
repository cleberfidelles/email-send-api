package com.fidellesdev.email_send.kafka;

import com.fidellesdev.email_send.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailService emailService;

    @Test
    @DisplayName("EmailConsumer: converte o payload e delega ao service")
    void consumir_deveDelegarAoService() {
        new EmailConsumer(emailService).consumir(" 42 ");

        verify(emailService).processarEnvio(42L);
    }

    @Test
    @DisplayName("EmailConsumer: payload inválido lança NumberFormatException (vai direto para a DLT)")
    void consumir_comPayloadInvalido() {
        EmailConsumer consumer = new EmailConsumer(emailService);

        assertThrows(NumberFormatException.class, () -> consumer.consumir("abc"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("EmailDeadLetterConsumer: marca o e-mail como ERRO")
    void dlt_deveMarcarComoErro() {
        new EmailDeadLetterConsumer(emailService).consumir("7");

        verify(emailService).marcarComoErro(7L);
    }

    @Test
    @DisplayName("EmailDeadLetterConsumer: payload inválido é ignorado sem lançar exceção")
    void dlt_comPayloadInvalido_ignora() {
        new EmailDeadLetterConsumer(emailService).consumir("lixo");

        verifyNoInteractions(emailService);
    }
}
