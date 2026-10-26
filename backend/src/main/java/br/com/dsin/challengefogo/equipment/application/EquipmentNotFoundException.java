package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.shared.domain.NotFoundException;

import java.util.UUID;

public class EquipmentNotFoundException extends NotFoundException {

    public EquipmentNotFoundException(UUID id) {
        super("Equipamento " + id + " não encontrado.");
    }
}
