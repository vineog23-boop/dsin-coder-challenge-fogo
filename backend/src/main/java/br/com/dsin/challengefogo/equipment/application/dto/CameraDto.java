package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.CameraResolution;
import br.com.dsin.challengefogo.equipment.persistence.CameraEntity;
import jakarta.validation.constraints.NotNull;

public record CameraDto(
        @NotNull(message = "Informe se a câmera tem visão noturna.")
        Boolean nightVision,

        @NotNull(message = "A resolução da câmera é obrigatória.")
        CameraResolution resolution,

        @NotNull(message = "Informe se a câmera é colorida.")
        Boolean color,

        @NotNull(message = "Informe se a câmera gira e dá zoom (PTZ).")
        Boolean ptz) {

    public CameraDto(CameraEntity camera) {
        this(camera.isNightVision(), camera.getResolution(), camera.isColor(), camera.isPtz());
    }
}
