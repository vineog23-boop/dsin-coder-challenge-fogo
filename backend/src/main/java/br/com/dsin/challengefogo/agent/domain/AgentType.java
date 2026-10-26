package br.com.dsin.challengefogo.agent.domain;

import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.shared.domain.Agency;

/**
 * Tipo de agente. O órgão é derivado do tipo (não é escolhido), e cada tipo usa a viatura do seu órgão.
 */
public enum AgentType {
    TRAFFIC_AGENT(Agency.MUNICIPAL_TRAFFIC, EquipmentType.TRAFFIC_AGENT_MOTORCYCLE),
    MUNICIPAL_GUARD(Agency.MUNICIPAL_GUARD, EquipmentType.MUNICIPAL_GUARD_VEHICLE),
    MILITARY_POLICE(Agency.MILITARY_POLICE, EquipmentType.MILITARY_POLICE_VEHICLE);

    private final Agency agency;
    private final EquipmentType vehicleType;

    AgentType(Agency agency, EquipmentType vehicleType) {
        this.agency = agency;
        this.vehicleType = vehicleType;
    }

    public Agency agency() {
        return agency;
    }

    /** Tipo de veículo típico do órgão; o front usa para sugerir viaturas. */
    public EquipmentType vehicleType() {
        return vehicleType;
    }
}
