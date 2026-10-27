package br.com.dsin.challengefogo.equipment.persistence;

import java.time.Instant;
import java.util.UUID;

/** Última vez em que o equipamento entrou numa situação: o "desde" do alerta. */
public record StatusSinceView(UUID equipmentId, Instant since) {
}
