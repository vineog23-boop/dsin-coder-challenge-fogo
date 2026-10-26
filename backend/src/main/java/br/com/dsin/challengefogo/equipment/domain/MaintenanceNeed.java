package br.com.dsin.challengefogo.equipment.domain;

/** Necessidade de manutenção; {@code SCHEDULED} exige data da próxima manutenção. */
public enum MaintenanceNeed {
    NONE,
    IMMEDIATE,
    SCHEDULED
}
