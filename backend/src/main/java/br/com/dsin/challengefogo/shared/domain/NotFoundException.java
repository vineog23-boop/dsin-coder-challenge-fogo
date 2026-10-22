package br.com.dsin.challengefogo.shared.domain;

/**
 * Recurso procurado não existe. Cada feature estende com uma exceção específica
 * (ex.: {@code EquipmentNotFoundException}) e a API responde {@code 404}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
