package br.com.dsin.challengefogo.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Páginas saem como {@code {content, page: {size, number, totalElements, totalPages}}}: formato estável,
 * em vez de serializar o {@code PageImpl} interno do Spring Data.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {

    static final int MAX_PAGE_SIZE = 100;

    /**
     * O limite fica aqui, e não em {@code spring.data.web.pageable.max-page-size}: com
     * {@code @EnableSpringDataWebSupport} a auto-configuração do Boot recua e ignora essa propriedade.
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
