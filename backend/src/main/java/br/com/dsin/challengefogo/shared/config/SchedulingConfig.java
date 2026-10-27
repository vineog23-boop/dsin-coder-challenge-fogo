package br.com.dsin.challengefogo.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o {@code @Scheduled}. Hoje só a telemetria simulada usa, e ela mesma só existe com a propriedade ligada. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
