package com.fidellesdev.email_send.kafka;

import com.fidellesdev.email_send.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Recebe o que esgotou as tentativas e marca o e-mail como ERRO. */
@Slf4j
@Component
public class EmailDeadLetterConsumer {

    private final EmailService emailService;

    public EmailDeadLetterConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = EmailProducer.TOPICO_DLT, groupId = "email-send-dlt-group")
    public void consumir(String emailId) {
        try {
            emailService.marcarComoErro(Long.valueOf(emailId.trim()));
        } catch (NumberFormatException e) {
            log.error("Mensagem inválida na DLT, ignorada: '{}'", emailId);
        }
    }
}
