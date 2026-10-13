package com.fidellesdev.email_send.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class EmailProducer {

    public static final String TOPICO = "email-envio";
    /** Tópico de mensagens que esgotaram as tentativas (nome padrão do DeadLetterPublishingRecoverer). */
    public static final String TOPICO_DLT = TOPICO + ".DLT";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EmailProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publica o id do e-mail (a chave é o próprio id). O resultado vem no future. */
    public CompletableFuture<SendResult<String, String>> publicar(Long emailId) {
        String id = String.valueOf(emailId);
        return kafkaTemplate.send(TOPICO, id, id);
    }
}
