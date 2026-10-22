# Tarefas: agent

Spec: `spec.md`. Plano: `.specs/plan-parte-1.md`.

## T18: Cadastro de agente e regras de veículo

Migration `agent` + `agent_status_history`; `AgentEntity` (`Location` embutida para a base, `vehicle` como `@ManyToOne` para `VehicleEntity`); `POST`, `GET {id}`, `GET` com filtros, `PUT`. Órgão derivado do tipo.

- [ ] Aceite: regra 2 da spec (veículo inexistente `404`; tipo, órgão ou situação incompatível `422`); matrícula duplicada `409`.
- [ ] Aceite: filtros `agentType`, `status`, `agency`, `region`, `search` combinados e paginados.
- Verificação: unitário (TDD, regras 1 e 2); integração dos filtros; MockMvc; `./mvnw test`.
- Arquivos: `V4__create_agent.sql`, `agent/persistence/*`, `agent/application/AgentService.java`, `agent/web/AgentController.java` + DTOs, testes.
- Depende de: T13.

## T19: Situação, posição e histórico do agente

`PATCH /status` (histórico; `OFF_DUTY`/`ON_LEAVE` desvincula o veículo), `PATCH /position` (operador pode), `GET /history`.

- [ ] Aceite: regras 4 e 5 da spec; operador atualiza posição e recebe `403` ao mudar a situação.
- Verificação: unitário (TDD); MockMvc; `./mvnw test`.
- Arquivos: `AgentStatusHistoryEntity.java` + repository, service, controller, DTOs, testes.
- Depende de: T18.

## T20: Front de agentes

`src/lib/api/agents.ts`; lista com filtros; formulário com seletor de veículo que só oferece compatíveis com o órgão; detalhe com histórico e mudança de situação (gestor).

- [ ] Aceite: criar e editar agente com veículo pelo navegador; erro `422` aparece no formulário.
- Verificação: lint, test, build; navegador com os dois perfis.
- Arquivos: `src/lib/api/agents.ts`, `src/pages/AgentListPage.tsx`, `src/pages/AgentFormPage.tsx`, `src/pages/AgentDetailPage.tsx`.
- Depende de: T19, T16.
- Corte possível se faltar tempo: detalhe sem linha do tempo.

## T21: Seed de agentes

`shared/demo/DemoAgentSeeder`: ~40 agentes, três órgãos, cinco regiões, parte com veículo compatível, ~6 meses de histórico; nomes fictícios; `Random(42)`.

- [ ] Aceite: idempotente e determinístico; todo vínculo respeita a regra 2.
- Verificação: teste de integração do seeder; `docker compose down -v && docker compose up --build`.
- Arquivos: `shared/demo/DemoAgentSeeder.java`, teste.
- Depende de: T19, T17.

## Checkpoint E
- [ ] Critérios de aceite de `auth`, `equipment` e `agent` marcados; seguir para T22 (`setup/tasks.md`).
