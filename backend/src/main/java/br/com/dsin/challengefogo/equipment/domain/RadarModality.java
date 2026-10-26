package br.com.dsin.challengefogo.equipment.domain;

/** Infração que o radar fiscaliza; só {@code SPEED} exige limite de velocidade. */
public enum RadarModality {
    SPEED,
    RED_LIGHT,
    ILLEGAL_TURN,
    ILLEGAL_U_TURN,
    BUS_LANE
}
