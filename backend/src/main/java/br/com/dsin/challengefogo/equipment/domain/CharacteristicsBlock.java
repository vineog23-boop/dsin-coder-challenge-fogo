package br.com.dsin.challengefogo.equipment.domain;

/**
 * Bloco de características de um grupo de tipos; corresponde a uma tabela filha.
 * {@code fieldName} é o nome do bloco no JSON, usado para apontar o campo nos erros.
 */
public enum CharacteristicsBlock {
    RADAR("radar"),
    CAMERA("camera"),
    TRUCK_SCALE("truckScale"),
    VEHICLE("vehicle"),
    SCOOTER("scooter"),
    NONE(null);

    private final String fieldName;

    CharacteristicsBlock(String fieldName) {
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
