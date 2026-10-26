package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.equipment.persistence.ScooterEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A operadora exigida em {@code SHARED} é conferida no service. */
public record ScooterDto(
        @NotNull(message = "Informe se o patinete é público ou compartilhado.")
        ScooterOwnership ownership,

        @Size(max = 100, message = "O nome da operadora deve ter no máximo 100 caracteres.")
        String operatorName,

        @Min(value = 0, message = "A bateria deve estar entre 0 e 100.")
        @Max(value = 100, message = "A bateria deve estar entre 0 e 100.")
        Integer batteryLevel) {

    public ScooterDto(ScooterEntity scooter) {
        this(scooter.getOwnership(), scooter.getOperatorName(), scooter.getBatteryLevel());
    }
}
