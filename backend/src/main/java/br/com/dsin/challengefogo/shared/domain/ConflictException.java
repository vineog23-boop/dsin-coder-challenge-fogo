package br.com.dsin.challengefogo.shared.domain;

/**
 * Operação conflita com um dado já existente (ex.: número de série ou placa duplicados).
 * A API responde {@code 409}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
