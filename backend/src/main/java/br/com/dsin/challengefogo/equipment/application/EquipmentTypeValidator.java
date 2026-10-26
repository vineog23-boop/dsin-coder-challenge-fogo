package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoRequest;
import br.com.dsin.challengefogo.equipment.domain.CharacteristicsBlock;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException.FieldViolation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras que cruzam campos e por isso não cabem em anotação: o bloco de características tem que
 * corresponder ao tipo (regra 1), {@code SCHEDULED} exige data (regra 6) e os campos condicionais
 * de radar e patinete. Junta todas as violações para o formulário marcar tudo de uma vez.
 */
final class EquipmentTypeValidator {

    private EquipmentTypeValidator() {
    }

    static void validate(EquipmentDtoRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        CharacteristicsBlock expected = request.type().block();

        blocksPresent(request).forEach((block, present) -> {
            if (block == expected && !present) {
                violations.add(new FieldViolation(block.fieldName(),
                        "As características são obrigatórias para o tipo " + request.type() + "."));
            } else if (block != expected && present) {
                violations.add(new FieldViolation(block.fieldName(),
                        "Essas características não se aplicam ao tipo " + request.type() + "."));
            }
        });

        if (expected == CharacteristicsBlock.RADAR && request.radar() != null
                && request.radar().modality() == RadarModality.SPEED && request.radar().speedLimitKmh() == null) {
            violations.add(new FieldViolation("radar.speedLimitKmh",
                    "O limite de velocidade é obrigatório para radar de velocidade."));
        }
        if (expected == CharacteristicsBlock.SCOOTER && request.scooter() != null
                && request.scooter().ownership() == ScooterOwnership.SHARED
                && (request.scooter().operatorName() == null || request.scooter().operatorName().isBlank())) {
            violations.add(new FieldViolation("scooter.operatorName",
                    "A operadora é obrigatória para patinete compartilhado."));
        }
        if (request.maintenanceNeedOrDefault() == MaintenanceNeed.SCHEDULED && request.nextMaintenanceDate() == null) {
            violations.add(new FieldViolation("nextMaintenanceDate",
                    "A data da próxima manutenção é obrigatória quando a manutenção está agendada."));
        }

        if (!violations.isEmpty()) {
            throw new FieldValidationException(violations);
        }
    }

    private static Map<CharacteristicsBlock, Boolean> blocksPresent(EquipmentDtoRequest request) {
        Map<CharacteristicsBlock, Boolean> present = new LinkedHashMap<>();
        present.put(CharacteristicsBlock.RADAR, request.radar() != null);
        present.put(CharacteristicsBlock.CAMERA, request.camera() != null);
        present.put(CharacteristicsBlock.TRUCK_SCALE, request.truckScale() != null);
        present.put(CharacteristicsBlock.VEHICLE, request.vehicle() != null);
        present.put(CharacteristicsBlock.SCOOTER, request.scooter() != null);
        return present;
    }
}
