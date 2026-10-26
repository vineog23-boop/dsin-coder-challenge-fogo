package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.shared.domain.NotFoundException;

import java.util.UUID;

/** Viatura informada no agente não existe (regra 2: {@code 404}, separado da incompatibilidade, que é {@code 422}). */
public class VehicleNotFoundException extends NotFoundException {

    public VehicleNotFoundException(UUID id) {
        super("Veículo " + id + " não encontrado.");
    }
}
