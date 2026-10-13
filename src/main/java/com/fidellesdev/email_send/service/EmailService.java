package com.fidellesdev.email_send.service;

import com.fidellesdev.email_send.dtos.EmailRequestDto;
import com.fidellesdev.email_send.dtos.EmailResponseDto;
import com.fidellesdev.email_send.enums.StatusEnvio;
import com.fidellesdev.email_send.exception.EmailEnvioException;
import com.fidellesdev.email_send.exception.EmailNaoEncontradoException;
import com.fidellesdev.email_send.kafka.EmailProducer;
import com.fidellesdev.email_send.model.Email;
import com.fidellesdev.email_send.repository.EmailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final JavaMailSender mailSender;
    private final EmailProducer emailProducer;
    private final String remetente;

    public EmailService(EmailRepository emailRepository,
                        JavaMailSender mailSender,
                        EmailProducer emailProducer,
                        @Value("${spring.mail.username}") String remetente) {
        this.emailRepository = emailRepository;
        this.mailSender = mailSender;
        this.emailProducer = emailProducer;
        this.remetente = remetente;
    }

    /** Chamado pelo controller: salva como PENDENTE e publica o id no Kafka. */
    public EmailResponseDto solicitarEnvio(EmailRequestDto dto) {
        Email email = new Email();
        email.setAssunto(dto.assunto());
        email.setDestinatario(dto.destinatario());
        email.setMensagem(dto.mensagem());
        email.setStatusEnvio(StatusEnvio.PENDENTE);
        email.setDataCriacao(LocalDateTime.now());

        Email salvo = emailRepository.save(email);
        publicar(salvo.getId());
        return EmailResponseDto.fromEntity(salvo);
    }

    public EmailResponseDto buscarPorId(Long id) {
        return emailRepository.findById(id)
                .map(EmailResponseDto::fromEntity)
                .orElseThrow(() -> new EmailNaoEncontradoException(id));
    }

    /**
     * Chamado pelo consumer: envia de verdade e atualiza o status.
     * Se o SMTP falhar, lança EmailEnvioException para o Kafka tentar de novo;
     * o status só vira ERRO quando as tentativas se esgotam (ver marcarComoErro).
     */
    public void processarEnvio(Long emailId) {
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email == null) {
            log.warn("E-mail id={} não existe no banco; mensagem ignorada", emailId);
            return;
        }
        if (email.getStatusEnvio() != StatusEnvio.PENDENTE) {
            log.info("E-mail id={} já processado (status {}); ignorado", emailId, email.getStatusEnvio());
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(email.getDestinatario());
            msg.setSubject(email.getAssunto());
            msg.setText(email.getMensagem());
            mailSender.send(msg);
        } catch (MailException e) {
            log.warn("Falha ao enviar e-mail id={}: {}", emailId, e.getMessage());
            throw new EmailEnvioException("Falha ao enviar e-mail id=" + emailId, e);
        }

        email.setStatusEnvio(StatusEnvio.ENVIADO);
        email.setDataEnvio(LocalDateTime.now());
        emailRepository.save(email);
        log.info("E-mail id={} enviado", emailId);
    }

    /** Chamado quando o e-mail esgotou as tentativas (DLT) ou não conseguiu ser publicado. */
    public void marcarComoErro(Long emailId) {
        emailRepository.findById(emailId).ifPresentOrElse(email -> {
            if (email.getStatusEnvio() == StatusEnvio.PENDENTE) {
                email.setStatusEnvio(StatusEnvio.ERRO);
                emailRepository.save(email);
                log.error("E-mail id={} marcado como ERRO", emailId);
            }
        }, () -> log.warn("E-mail id={} não encontrado ao marcar como ERRO", emailId));
    }

    private void publicar(Long emailId) {
        try {
            emailProducer.publicar(emailId).whenComplete((resultado, erro) -> {
                if (erro != null) {
                    falhaNaPublicacao(emailId, erro);
                }
            });
        } catch (RuntimeException e) {
            falhaNaPublicacao(emailId, e);
        }
    }

    private void falhaNaPublicacao(Long emailId, Throwable erro) {
        log.error("Não foi possível publicar o e-mail id={} no Kafka", emailId, erro);
        marcarComoErro(emailId);
    }
}
