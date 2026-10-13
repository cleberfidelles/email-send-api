package com.fidellesdev.email_send.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailRequestDto(
        @NotBlank(message = "Informe o assunto")
        @Size(max = 255, message = "O assunto deve ter no máximo 255 caracteres")
        @Pattern(regexp = "^[^\\r\\n]*$", message = "O assunto não pode conter quebras de linha")
        String assunto,

        @NotBlank(message = "Informe o e-mail do destinatário")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String destinatario,

        @NotBlank(message = "Informe a mensagem")
        @Size(max = 10000, message = "A mensagem deve ter no máximo 10000 caracteres")
        String mensagem
) {
}
