# Tarefas: operations

Spec: `spec.md`. Plano: `.specs/plan-parte-2.md`.

## G4: API da central

### T23: Consultas de agregação + `GET /summary` e `GET /map`

Projeções e `@Query` em `EquipmentRepository` e `AgentRepository`:
- contagem por situação;
- manutenção imediata;
- manutenção atrasada (`SCHEDULED` e data anterior a hoje);
- falhas e agentes em atividade por região;
- marcadores leves com filtros.

`OperationsService.summary()`/`map(filter)` com `Clock`. `OperationsController` em `/api/operations`. DTOs `SummaryDtoResponse`, `MapDtoResponse` (com `truncated`, teto de 2.000 por camada).

- [x] Aceite: os números do `/summary` batem com dados de teste conhecidos (integração), inclusive manutenção atrasada pela data de hoje do `Clock`.
- [x] Aceite: o `/map` aplica a regra 1 (posição atual ou base, com `positionSource`) e a regra 2 (por padrão só agentes em atividade); os filtros `equipmentType`, `equipmentStatus`, `agentStatus`, `region` e `agency` funcionam combinados.
- Verificação: unitário do service (regra 1, `truncated`); `OperationsQueriesTest` (Testcontainers); MockMvc (dois perfis, `401`); `./mvnw test`.
- Arquivos: `operations/web/OperationsController.java`, `operations/application/OperationsService.java` + DTOs, projeções e queries em `EquipmentRepository`/`AgentRepository`, testes.
- Depende de: Parte 1.

### T24: Alertas calculados + `GET /alerts`

`AlertService` monta os alertas da regra 5 (falha, roubado, região sem agente em atividade, manutenção atrasada), com mensagem em português, região, `equipmentId` e "desde" (`max(changed_at)` da última mudança para a situação). Ordem: severidade e depois o mais antigo. Paginação em memória (máx. 100).

- [x] Aceite: regra 5 completa: ordem, severidades, `DEACTIVATED` não alerta, região sem agente em atividade gera alerta, "desde" correto.
- Verificação: unitário (TDD, `Clock` fixo); integração da consulta do "desde"; MockMvc; `./mvnw test`.
- Arquivos: `operations/application/AlertService.java`, `AlertDtoResponse.java`, query do "desde" em `EquipmentStatusHistoryRepository`, controller, testes.
- Depende de: T23.

### T25: Telemetria simulada

`TelemetrySimulator` com `@Scheduled(fixedDelay = 10 s)`, `@EnableScheduling` e `@ConditionalOnProperty(app.telemetry.enabled)`.
- Move cada agente em atividade até ~150 m por vez e nunca a mais de ~1,5 km da base.
- Atualiza `positionUpdatedAt` pelo `Clock`.
- Não grava histórico.
- O compose liga com `APP_TELEMETRY_ENABLED=true`.

- [x] Aceite: regra 6: o agente em atividade se move dentro do raio, o fora de serviço não se move e nenhuma linha nova aparece em `agent_status_history`.
- [x] Aceite: os testes com perfil `demo` continuam sem o agendamento ligado.
- Verificação: unitário (TDD, `Random` com semente e `Clock` fixos); `./mvnw test`; no compose, a posição de um agente muda entre duas chamadas espaçadas de 10 s.
- Arquivos: `operations/application/TelemetrySimulator.java`, `shared/config/SchedulingConfig.java`, `docker-compose.yml`, teste.
- Depende de: T23.

### Checkpoint F
- [x] `./mvnw test` verde (166); os 3 GETs conferidos no compose com os dois perfis e batendo com SQL direto; telemetria movendo agentes sem gravar histórico.

## G5: Central no navegador

### T26: Página Central (mapa, filtros e polling)

`src/lib/api/operations.ts`, `src/lib/mapIcons.ts` (ícone por tipo, cor por situação, `L.divIcon`), `src/lib/usePolling.ts` (10 s, pausa com a aba oculta), `OperationsMap` (marcadores de equipamento e de agente com popup e link), `MapLegend` e filtros na URL. Rota `/central`.

- [ ] Aceite: os 150 equipamentos e os agentes em atividade aparecem no mapa com ícone por tipo e cor por situação; o popup tem resumo e link para o detalhe.
- [ ] Aceite: `equipmentStatus=MALFUNCTION&region=NORTH` filtra o mapa e fica na URL; o polling atualiza sem recarregar e mostra "atualizado há X s".
- Verificação: `mapIcons.test.ts`, `usePolling.test.ts`; lint e build; navegador (celular e desktop).
- Arquivos: `src/lib/api/operations.ts`, `src/lib/mapIcons.ts`, `src/lib/usePolling.ts`, `src/components/OperationsMap.tsx`, `src/components/MapLegend.tsx`, `src/pages/OperationsPage.tsx`, testes.
- Depende de: T23 (contrato do `/map`).

### T27: Indicadores e alertas na Central

`SummaryCards` (com falha, manutenção imediata, manutenção atrasada, agentes em atividade), `RegionTable`, `AlertList` (severidade, mensagem, tempo decorrido, link), todos no mesmo ciclo de polling.

- [ ] Aceite: os cartões e a tabela batem com o `/summary`; os alertas aparecem com link para o detalhe.
- [ ] Aceite: falha de rede mantém os últimos dados com aviso; com a lista vazia, aparece mensagem de "nenhum alerta".
- Verificação: `OperationsPage.test.tsx` (mapa mockado: filtros na URL, cartões, alertas com link, erro mantém os dados); lint e build; navegador.
- Arquivos: `src/components/SummaryCards.tsx`, `src/components/RegionTable.tsx`, `src/components/AlertList.tsx`, `src/pages/OperationsPage.tsx`, teste.
- Depende de: T24, T26.
- Corte possível: sem `RegionTable` (só cartões).

### T28: Tela inicial, README e verificação do zero

`/central` vira a tela inicial (menu "Central · Equipamentos · Agentes"; redirecionamentos do login e de rota desconhecida). README: seção "Central", Swagger com as rotas novas e roteiro do vídeo atualizado.

- [ ] Aceite: o login leva a `/central`; rota desconhecida também.
- [ ] Aceite: do zero (`docker compose down -v && up --build`), a central abre com dados, e mudar um equipamento para falha aparece em até 10 s (marcador vermelho, indicador e alerta).
- Verificação: testes de rota existentes ajustados; `npm test`, lint e build; `./mvnw test`; roteiro manual no navegador.
- Arquivos: `src/App.tsx`, `src/pages/LoginPage.tsx`, `README.md`.
- Depende de: T27, T25.

### Checkpoint G
- [ ] Todos os critérios de aceite da spec `operations` conferidos e marcados; `./mvnw test`, `npm test`, lint e build verdes.
