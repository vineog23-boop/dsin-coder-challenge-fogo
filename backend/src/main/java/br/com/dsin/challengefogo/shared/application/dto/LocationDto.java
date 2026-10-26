package br.com.dsin.challengefogo.shared.application.dto;

import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Localização na entrada e na saída da API; reaproveitada por equipamento e agente. */
public record LocationDto(
        @Pattern(regexp = "\\d{8}", message = "O CEP deve ter 8 dígitos, sem hífen.")
        String zipCode,

        @NotBlank(message = "A rua é obrigatória.")
        @Size(max = 120, message = "A rua deve ter no máximo 120 caracteres.")
        String street,

        @Size(max = 120, message = "A referência deve ter no máximo 120 caracteres.")
        String reference,

        @NotBlank(message = "O bairro é obrigatório.")
        @Size(max = 80, message = "O bairro deve ter no máximo 80 caracteres.")
        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória.")
        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
        String city,

        @NotNull(message = "A UF é obrigatória.")
        @Pattern(regexp = "[A-Z]{2}", message = "A UF deve ter 2 letras maiúsculas.")
        String state,

        @NotNull(message = "A região é obrigatória.")
        Region region,

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

    public LocationDto(Location location) {
        this(location.getZipCode(), location.getStreet(), location.getReference(), location.getNeighborhood(),
                location.getCity(), location.getState(), location.getRegion(), location.getLatitude(),
                location.getLongitude());
    }

    public Location toLocation() {
        return new Location(zipCode, street, reference, neighborhood, city, state, region, latitude, longitude);
    }
}
