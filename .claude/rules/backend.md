---
paths:
  - "backend/**"
---

# Regras do back-end

- Migrations Flyway em `src/main/resources/db/migration/V<n>__descricao.sql`. Migration já aplicada nunca é editada; mudança vira nova migration.
- Tipo e situação de equipamento são `enum`.
- Características por tipo de equipamento: herança JPA `JOINED` (`equipment` + tabelas filhas `radar`, `camera`, `truck_scale`, `vehicle`, `scooter`). Não usar JSONB.
- `@Transactional` no service, em métodos que escrevem.
- Coordenadas GPS: `BigDecimal`, coluna `numeric(9,6)`.
- Mudança de situação sempre grava histórico (`*_status_history`); nada é excluído fisicamente.
