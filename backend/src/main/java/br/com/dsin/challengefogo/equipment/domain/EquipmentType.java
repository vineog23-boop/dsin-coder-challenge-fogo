package br.com.dsin.challengefogo.equipment.domain;

/** Tipo de equipamento; define qual tabela filha guarda as características (ou nenhuma). */
public enum EquipmentType {
    RADAR(CharacteristicsBlock.RADAR),
    SURVEILLANCE_CAMERA(CharacteristicsBlock.CAMERA),
    TRUCK_SCALE(CharacteristicsBlock.TRUCK_SCALE),
    TAXI(CharacteristicsBlock.VEHICLE),
    MOTO_TAXI(CharacteristicsBlock.VEHICLE),
    SCHOOL_TRANSPORT(CharacteristicsBlock.VEHICLE),
    PARKING_ENFORCEMENT_VEHICLE(CharacteristicsBlock.VEHICLE),
    TRAFFIC_AGENT_MOTORCYCLE(CharacteristicsBlock.VEHICLE),
    MUNICIPAL_GUARD_VEHICLE(CharacteristicsBlock.VEHICLE),
    MILITARY_POLICE_VEHICLE(CharacteristicsBlock.VEHICLE),
    ELECTRIC_SCOOTER(CharacteristicsBlock.SCOOTER),
    AUTOMATED_GATE(CharacteristicsBlock.NONE),
    PARKING_KIOSK(CharacteristicsBlock.NONE),
    OTHER(CharacteristicsBlock.NONE);

    private final CharacteristicsBlock block;

    EquipmentType(CharacteristicsBlock block) {
        this.block = block;
    }

    public CharacteristicsBlock block() {
        return block;
    }
}
