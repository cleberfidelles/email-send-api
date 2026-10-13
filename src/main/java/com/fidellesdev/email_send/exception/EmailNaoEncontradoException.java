package com.fidellesdev.email_send.exception;

public class EmailNaoEncontradoException extends RuntimeException {

    public EmailNaoEncontradoException(Long id) {
        super("E-mail não encontrado: id " + id);
    }
}
