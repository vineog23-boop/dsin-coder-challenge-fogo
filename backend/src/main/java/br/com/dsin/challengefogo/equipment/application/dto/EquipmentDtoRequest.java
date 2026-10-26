package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.domain.Agency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Criação e edição. Só o bloco de características do tipo deve vir preenchido; a combinação
 * tipo ↔ bloco é conferida no service. {@code status} vale só na criação (padrão {@code NOT_INSTALLED});
 * depois, a situação muda pelo {@code PATCH /status}, que grava histórico.
 */
public record EquipmentDtoRequest(
        @NotNull(message = "O tipo é obrigatório.")
        EquipmentType type,

        @NotBlank(message = "O número de série é obrigatório.")
        @Size(max = 60, message = "O número de série deve ter no máximo 60 caracteres.")
        String serialNumber,

        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 60, message = "A marca deve ter no máximo 60 caracteres.")
        String brand,

        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 60, message = "O modelo deve ter no máximo 60 caracteres.")
        String model,

        @NotNull(message = "O órgão responsável é obrigatório.")
        Agency agency,

        EquipmentStatus status,

        MaintenanceNeed maintenanceNeed,

        LocalDate nextMaintenanceDate,

        @Positive(message = "O intervalo de manutenção deve ser positivo.")
        Integer maintenanceIntervalDays,

        @NotNull(message = "A localização é obrigatória.")
        @Valid
        LocationDto location,

        @Valid RadarDto radar,
        @Valid CameraDto camera,
        @Valid TruckScaleDto truckScale,
        @Valid VehicleDto vehicle,
        @Valid ScooterDto scooter) {

    /** Sem valor informado, não há manutenção pendente. */
    public MaintenanceNeed maintenanceNeedOrDefault() {
        return maintenanceNeed != null ? maintenanceNeed : MaintenanceNeed.NONE;
    }
}
