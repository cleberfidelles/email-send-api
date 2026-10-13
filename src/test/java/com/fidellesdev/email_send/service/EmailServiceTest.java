package com.fidellesdev.email_send.service;

import com.fidellesdev.email_send.dtos.EmailRequestDto;
import com.fidellesdev.email_send.dtos.EmailResponseDto;
import com.fidellesdev.email_send.enums.StatusEnvio;
import com.fidellesdev.email_send.exception.EmailEnvioException;
import com.fidellesdev.email_send.exception.EmailNaoEncontradoException;
import com.fidellesdev.email_send.kafka.EmailProducer;
import com.fidellesdev.email_send.model.Email;
import com.fidellesdev.email_send.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String REMETENTE = "remetente@teste.com";

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailProducer emailProducer;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailRepository, mailSender, emailProducer, REMETENTE);
    }

    private Email criarEmail(Long id, StatusEnvio status) {
        Email email = new Email();
        ReflectionTestUtils.setField(email, "id", id);
        email.setAssunto("Assunto");
        email.setDestinatario("destino@teste.com");
        email.setMensagem("Mensagem");
        email.setDataCriacao(LocalDateTime.now());
        email.setStatusEnvio(status);
        return email;
    }

    // ---------------- solicitarEnvio ----------------

    @Test
    @DisplayName("solicitarEnvio: salva como PENDENTE e publica o id no Kafka")
    void solicitarEnvio_deveSalvarComoPendenteEPublicar() {
        EmailRequestDto dto = new EmailRequestDto("Assunto", "destino@teste.com", "Mensagem");
        when(emailRepository.save(any(Email.class))).thenAnswer(inv -> {
            Email e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 1L);
            return e;
        });
        when(emailProducer.publicar(1L)).thenReturn(CompletableFuture.completedFuture(null));

        EmailResponseDto resposta = emailService.solicitarEnvio(dto);

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(captor.capture());
        assertEquals(StatusEnvio.PENDENTE, captor.getValue().getStatusEnvio());
        assertEquals("destino@teste.com", captor.getValue().getDestinatario());
        assertNotNull(captor.getValue().getDataCriacao());
        assertNull(captor.getValue().getDataEnvio());

        verify(emailProducer).publicar(1L);
        verifyNoInteractions(mailSender);

        assertEquals(1L, resposta.id());
        assertEquals(StatusEnvio.PENDENTE, resposta.statusEnvio());
    }

    @Test
    @DisplayName("solicitarEnvio: se a publicação no Kafka falhar (future), marca ERRO")
    void solicitarEnvio_quandoPublicacaoFalha_deveMarcarErro() {
        Email salvo = criarEmail(1L, StatusEnvio.PENDENTE);
        when(emailRepository.save(any(Email.class))).thenReturn(salvo);
        when(emailProducer.publicar(1L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka fora")));
        when(emailRepository.findById(1L)).thenReturn(Optional.of(salvo));

        emailService.solicitarEnvio(new EmailRequestDto("Assunto", "destino@teste.com", "Mensagem"));

        assertEquals(StatusEnvio.ERRO, salvo.getStatusEnvio());
        verify(emailRepository, times(2)).save(any(Email.class));
    }

    @Test
    @DisplayName("solicitarEnvio: se o producer lançar exceção, marca ERRO")
    void solicitarEnvio_quandoProducerLancaExcecao_deveMarcarErro() {
        Email salvo = criarEmail(1L, StatusEnvio.PENDENTE);
        when(emailRepository.save(any(Email.class))).thenReturn(salvo);
        when(emailProducer.publicar(1L)).thenThrow(new RuntimeException("sem broker"));
        when(emailRepository.findById(1L)).thenReturn(Optional.of(salvo));

        emailService.solicitarEnvio(new EmailRequestDto("Assunto", "destino@teste.com", "Mensagem"));

        assertEquals(StatusEnvio.ERRO, salvo.getStatusEnvio());
    }

    // ---------------- buscarPorId ----------------

    @Test
    @DisplayName("buscarPorId: retorna o DTO quando existe")
    void buscarPorId_quandoExiste() {
        when(emailRepository.findById(5L)).thenReturn(Optional.of(criarEmail(5L, StatusEnvio.ENVIADO)));

        EmailResponseDto resposta = emailService.buscarPorId(5L);

        assertEquals(5L, resposta.id());
        assertEquals(StatusEnvio.ENVIADO, resposta.statusEnvio());
    }

    @Test
    @DisplayName("buscarPorId: lança EmailNaoEncontradoException quando não existe")
    void buscarPorId_quandoNaoExiste() {
        when(emailRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmailNaoEncontradoException.class, () -> emailService.buscarPorId(99L));
    }

    // ---------------- processarEnvio ----------------

    @Test
    @DisplayName("processarEnvio: envia o e-mail e atualiza para ENVIADO com dataEnvio")
    void processarEnvio_deveEnviarEAtualizarParaEnviado() {
        Email email = criarEmail(1L, StatusEnvio.PENDENTE);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(email));

        emailService.processarEnvio(1L);

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());
        SimpleMailMessage enviada = msgCaptor.getValue();
        assertEquals(REMETENTE, enviada.getFrom());
        assertEquals("destino@teste.com", enviada.getTo()[0]);
        assertEquals("Assunto", enviada.getSubject());
        assertEquals("Mensagem", enviada.getText());

        assertEquals(StatusEnvio.ENVIADO, email.getStatusEnvio());
        assertNotNull(email.getDataEnvio());
        verify(emailRepository).save(email);
    }

    @Test
    @DisplayName("processarEnvio: falha de SMTP lança EmailEnvioException e mantém PENDENTE (para retry)")
    void processarEnvio_quandoSmtpFalha_deveLancarExcecaoEManterPendente() {
        Email email = criarEmail(2L, StatusEnvio.PENDENTE);
        when(emailRepository.findById(2L)).thenReturn(Optional.of(email));
        doThrow(new MailSendException("falha no smtp"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(EmailEnvioException.class, () -> emailService.processarEnvio(2L));

        assertEquals(StatusEnvio.PENDENTE, email.getStatusEnvio());
        assertNull(email.getDataEnvio());
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    @DisplayName("processarEnvio: ignora e-mail que já foi processado")
    void processarEnvio_quandoNaoEstaPendente_naoDeveEnviar() {
        Email email = criarEmail(3L, StatusEnvio.ENVIADO);
        when(emailRepository.findById(3L)).thenReturn(Optional.of(email));

        emailService.processarEnvio(3L);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    @DisplayName("processarEnvio: ignora id que não existe no banco")
    void processarEnvio_quandoNaoExiste_naoDeveFazerNada() {
        when(emailRepository.findById(99L)).thenReturn(Optional.empty());

        emailService.processarEnvio(99L);

        verifyNoInteractions(mailSender);
        verify(emailRepository, never()).save(any(Email.class));
    }

    // ---------------- marcarComoErro ----------------

    @Test
    @DisplayName("marcarComoErro: PENDENTE vira ERRO")
    void marcarComoErro_quandoPendente() {
        Email email = criarEmail(4L, StatusEnvio.PENDENTE);
        when(emailRepository.findById(4L)).thenReturn(Optional.of(email));

        emailService.marcarComoErro(4L);

        assertEquals(StatusEnvio.ERRO, email.getStatusEnvio());
        verify(emailRepository).save(email);
    }

    @Test
    @DisplayName("marcarComoErro: não altera e-mail que já foi ENVIADO")
    void marcarComoErro_quandoJaEnviado_naoAltera() {
        Email email = criarEmail(5L, StatusEnvio.ENVIADO);
        when(emailRepository.findById(5L)).thenReturn(Optional.of(email));

        emailService.marcarComoErro(5L);

        assertEquals(StatusEnvio.ENVIADO, email.getStatusEnvio());
        verify(emailRepository, never()).save(any(Email.class));
    }
}
