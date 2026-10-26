package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleDto(
        // Mercosul (ABC1D23) ou antigo (ABC1234), sem hífen.
        @NotNull(message = "A placa é obrigatória.")
        @Pattern(regexp = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}",
                message = "A placa deve estar no padrão ABC1D23 ou ABC1234, sem hífen.")
        String licensePlate,

        @PositiveOrZero(message = "A capacidade de passageiros não pode ser negativa.")
        Integer passengerCapacity,

        @NotNull(message = "Informe se o veículo é autônomo.")
        Boolean autonomous) {

    public VehicleDto(VehicleEntity vehicle) {
        this(vehicle.getLicensePlate(), vehicle.getPassengerCapacity(), vehicle.isAutonomous());
    }
}
