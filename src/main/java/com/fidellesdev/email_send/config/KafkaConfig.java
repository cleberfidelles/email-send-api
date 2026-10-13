package com.fidellesdev.email_send.config;

import com.fidellesdev.email_send.kafka.EmailProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(EmailProducer.TOPICO).partitions(1).replicas(1).build();
    }

    /** A DLT precisa ter ao menos as partições do tópico original (o recoverer usa a mesma partição). */
    @Bean
    public NewTopic emailDltTopic() {
        return TopicBuilder.name(EmailProducer.TOPICO_DLT).partitions(1).replicas(1).build();
    }

    /**
     * Retry com backoff exponencial (2s, 4s, 8s) e, depois, Dead Letter Topic.
     * O Spring Boot aplica automaticamente este bean aos @KafkaListener.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(2_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
        // Payload que não é número não melhora com retry: vai direto para a DLT
        handler.addNotRetryableExceptions(NumberFormatException.class);
        return handler;
    }
}
