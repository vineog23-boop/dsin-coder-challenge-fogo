package br.com.dsin.challengefogo.shared.domain;

/**
 * Requisição bem formada que viola uma regra de negócio (ex.: mudar para a mesma situação).
 * A API responde {@code 422}, separando do {@code 400} de validação de formato.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
