package br.com.dsin.challengefogo.equipment.domain;

/** Situação operacional. Sem exclusão física: o equipamento sai de uso com {@code DEACTIVATED}. */
public enum EquipmentStatus {
    NOT_INSTALLED,
    OPERATIONAL,
    MALFUNCTION,
    UNDER_MAINTENANCE,
    DEACTIVATED,
    STOLEN
}
