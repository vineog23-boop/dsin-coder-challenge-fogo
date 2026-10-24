package br.com.dsin.challengefogo.auth.application;

/**
 * Usuário inexistente ou senha errada. Um único tipo e uma única mensagem para os dois casos,
 * para a resposta não revelar quais usuários existem.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos.");
    }
}
