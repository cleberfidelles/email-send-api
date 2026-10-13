package com.fidellesdev.email_send.controller;

import com.fidellesdev.email_send.dtos.EmailRequestDto;
import com.fidellesdev.email_send.dtos.EmailResponseDto;
import com.fidellesdev.email_send.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/email")
@Tag(name = "Envio de Email", description = "API Rest para Envio de Email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    @Operation(summary = "Solicita o envio de um e-mail",
            description = "Registra o e-mail como PENDENTE e responde 202. O envio é assíncrono: "
                    + "acompanhe o resultado pelo GET /email/{id} (URL no header Location).")
    public ResponseEntity<EmailResponseDto> enviar(@RequestBody @Valid EmailRequestDto dto) {
        EmailResponseDto resposta = emailService.solicitarEnvio(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.accepted().location(location).body(resposta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o status de um e-mail",
            description = "Retorna PENDENTE, ENVIADO ou ERRO. 404 se o id não existir.")
    public EmailResponseDto buscar(@PathVariable Long id) {
        return emailService.buscarPorId(id);
    }
}
