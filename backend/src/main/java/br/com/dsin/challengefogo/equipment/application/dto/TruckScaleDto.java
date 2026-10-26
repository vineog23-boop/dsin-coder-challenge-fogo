package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.persistence.TruckScaleEntity;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TruckScaleDto(
        @NotNull(message = "A capacidade máxima é obrigatória.")
        @Positive(message = "A capacidade máxima deve ser positiva.")
        @Digits(integer = 4, fraction = 2, message = "A capacidade aceita até 4 dígitos inteiros e 2 decimais.")
        BigDecimal maxCapacityTons,

        @NotNull(message = "Informe se a balança pesa em movimento.")
        Boolean weighInMotion) {

    public TruckScaleDto(TruckScaleEntity scale) {
        this(scale.getMaxCapacityTons(), scale.isWeighInMotion());
    }
}
