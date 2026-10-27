package br.com.dsin.challengefogo.shared.persistence;

import br.com.dsin.challengefogo.shared.domain.Region;

/** Contagem agrupada por região, devolvida direto pela consulta (sem carregar entidades). */
public record RegionCount(Region region, long total) {
}
