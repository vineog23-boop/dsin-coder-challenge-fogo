package br.com.dsin.challengefogo.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Relógio como bean para que regras dependentes de tempo sejam testáveis com {@code Clock.fixed}. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
