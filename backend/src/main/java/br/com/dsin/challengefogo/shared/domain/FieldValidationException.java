package br.com.dsin.challengefogo.shared.domain;

import java.util.List;

/**
 * Validação que depende de mais de um campo (ex.: bloco de características que não combina com o tipo)
 * e por isso não cabe em anotação de Bean Validation. A API responde {@code 400} no mesmo formato
 * da validação comum, apontando o campo.
 */
public class FieldValidationException extends RuntimeException {

    private final List<FieldViolation> violations;

    public FieldValidationException(List<FieldViolation> violations) {
        super("Um ou mais campos estão inválidos.");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    public record FieldViolation(String field, String message) {
    }
}
