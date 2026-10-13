package com.fidellesdev.email_send.exception;

/**
 * Falha ao enviar o e-mail pelo SMTP. É lançada de propósito para o Kafka
 * fazer retry com backoff e, esgotadas as tentativas, mandar para a DLT.
 */
public class EmailEnvioException extends RuntimeException {

    public EmailEnvioException(String message, Throwable cause) {
        super(message, cause);
    }
}
