package com.fidellesdev.email_send.kafka;

import com.fidellesdev.email_send.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Se lançar exceção, o DefaultErrorHandler (KafkaConfig) faz retry com backoff
     * e, esgotadas as tentativas, envia a mensagem para a DLT.
     */
    @KafkaListener(topics = EmailProducer.TOPICO)
    public void consumir(String emailId) {
        log.debug("Mensagem recebida no tópico {}: id={}", EmailProducer.TOPICO, emailId);
        emailService.processarEnvio(Long.valueOf(emailId.trim()));
    }
}
