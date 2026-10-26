package br.com.dsin.challengefogo.agent.domain;

/** Situação do agente. "Em atividade" no painel = {@code AVAILABLE} + {@code ON_OCCURRENCE}. */
public enum AgentStatus {
    AVAILABLE,
    ON_OCCURRENCE,
    OFF_DUTY,
    ON_LEAVE;

    /** Só agente em serviço pode estar com viatura (regra 5). */
    public boolean isOnDuty() {
        return this == AVAILABLE || this == ON_OCCURRENCE;
    }
}
