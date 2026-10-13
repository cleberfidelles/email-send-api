package com.fidellesdev.email_send.dtos;

import com.fidellesdev.email_send.enums.StatusEnvio;
import com.fidellesdev.email_send.model.Email;

import java.time.LocalDateTime;

public record EmailResponseDto(
        Long id,
        String assunto,
        String destinatario,
        String mensagem,
        StatusEnvio statusEnvio,
        LocalDateTime dataCriacao,
        LocalDateTime dataEnvio
) {
    public static EmailResponseDto fromEntity(Email email) {
        return new EmailResponseDto(
                email.getId(),
                email.getAssunto(),
                email.getDestinatario(),
                email.getMensagem(),
                email.getStatusEnvio(),
                email.getDataCriacao(),
                email.getDataEnvio()
        );
    }
}
