package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Características de radar, na entrada e na saída. O limite exigido em {@code SPEED} é conferido no service. */
public record RadarDto(
        @NotNull(message = "A modalidade do radar é obrigatória.")
        RadarModality modality,

        @Positive(message = "O limite de velocidade deve ser positivo.")
        Integer speedLimitKmh,

        @Positive(message = "O número de faixas deve ser positivo.")
        Integer lanes) {

    public RadarDto(RadarEntity radar) {
        this(radar.getModality(), radar.getSpeedLimitKmh(), radar.getLanes());
    }
}
