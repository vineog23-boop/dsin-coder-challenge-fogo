package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusChangeDtoRequest(
        @NotNull(message = "A nova situação é obrigatória.")
        EquipmentStatus status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String note) {
}
