package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Random;

/**
 * Telemetria simulada (regra 6 da spec operations): a cada intervalo, cada agente em atividade dá um passo
 * aleatório perto da base. Só liga com {@code app.telemetry.enabled=true} (o compose liga, os testes não).
 * Não grava histórico: posição não é mudança de situação.
 */
@Component
@ConditionalOnProperty(name = "app.telemetry.enabled", havingValue = "true")
public class TelemetrySimulator {

    /** ~150 m por passo, em graus (aproximação suficiente na escala de uma cidade). */
    static final double STEP_DEGREES = 0.0013;
    /** ~1,5 km: o agente nunca se afasta mais que isso da base. */
    static final double MAX_DISTANCE_DEGREES = 0.0135;

    private final AgentRepository agentRepository;
    private final Clock clock;
    private final Random random;

    @Autowired
    public TelemetrySimulator(AgentRepository agentRepository, Clock clock) {
        this(agentRepository, clock, new Random());
    }

    TelemetrySimulator(AgentRepository agentRepository, Clock clock, Random random) {
        this.agentRepository = agentRepository;
        this.clock = clock;
        this.random = random;
    }

    @Scheduled(fixedDelayString = "${app.telemetry.interval-ms:10000}")
    @Transactional
    public void tick() {
        for (AgentEntity agent : agentRepository.findByStatusIn(OperationsService.ACTIVE_AGENTS)) {
            if (agent.getStatus().isOnDuty()) {
                move(agent);
            }
        }
    }

    /** Passo em direção aleatória a partir da posição atual (ou da base); longe demais, volta para a borda do raio. */
    private void move(AgentEntity agent) {
        double baseLat = agent.getBaseLocation().getLatitude().doubleValue();
        double baseLng = agent.getBaseLocation().getLongitude().doubleValue();
        double lat = agent.getCurrentLatitude() != null ? agent.getCurrentLatitude().doubleValue() : baseLat;
        double lng = agent.getCurrentLongitude() != null ? agent.getCurrentLongitude().doubleValue() : baseLng;

        double angle = random.nextDouble() * 2 * Math.PI;
        double step = random.nextDouble() * STEP_DEGREES;
        lat += step * Math.sin(angle);
        lng += step * Math.cos(angle);

        double distance = Math.hypot(lat - baseLat, lng - baseLng);
        if (distance > MAX_DISTANCE_DEGREES) {
            double scale = MAX_DISTANCE_DEGREES / distance;
            lat = baseLat + (lat - baseLat) * scale;
            lng = baseLng + (lng - baseLng) * scale;
        }

        agent.updatePosition(toCoordinate(lat), toCoordinate(lng), clock.instant());
    }

    private static BigDecimal toCoordinate(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
