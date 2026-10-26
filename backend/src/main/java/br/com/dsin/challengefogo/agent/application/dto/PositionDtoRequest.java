package br.com.dsin.challengefogo.agent.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PositionDtoRequest(
        @NotNull(message = "A latitude é obrigatória.")
        @DecimalMin(value = "-90", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90", message = "A latitude deve estar entre -90 e 90.")
        @Digits(integer = 3, fraction = 6, message = "A latitude aceita no máximo 6 casas decimais.")
        BigDecimal latitude,

        @NotNull(message = "A longitude é obrigatória.")
        @DecimalMin(value = "-180", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180", message = "A longitude deve estar entre -180 e 180.")
        @Digits(integer = 3, fraction = 6, message = "A longitude aceita no máximo 6 casas decimais.")
        BigDecimal longitude) {
}
