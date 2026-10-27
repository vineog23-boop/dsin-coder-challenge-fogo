# Spec: operations (Parte 2: central de operações)

Status: aprovada em 2026-10-26

## Objetivo

Dar ao gestor e ao operador uma **visão ao vivo da cidade**, que é a tela que se abre ao entrar no sistema:

- **Mapa:** onde estão os equipamentos e os agentes em atividade, e em que situação cada um está.
- **Indicadores:** quantos equipamentos estão com falha, quantas manutenções estão atrasadas, quantos agentes estão em atividade e por região.
- **Alertas:** o que precisa de atenção agora.

Tudo é **derivado dos dados da Parte 1**: sem tabela nova e sem ação de escrita nesta tela. Para agir (mudar situação, registrar manutenção), o usuário segue para o detalhe do equipamento ou do agente, que já existe.

Versão enxuta por causa do prazo (27/10 23:59). Decisões tomadas na clarificação:
- **Telemetria:** o back-end move os agentes em serviço a cada 10 s, só no perfil `demo`.
- **Alertas:** calculados na hora, sem persistência e sem "marcar como lido".
- **Atualização:** polling a cada 10 s, sem SSE nem WebSocket.
- **Ocorrências:** fora do escopo.

## Modelo

Nenhuma tabela nova. As fontes são `equipment` (situação, manutenção, localização), `equipment_status_history` (desde quando está em falha), `agent` (situação, base, posição atual, viatura) e `vehicle`.

### Regras

1. **Posição do agente no mapa:** a posição atual (`currentLatitude/Longitude`), se houver; senão, a base. A resposta informa qual das duas está sendo usada (regra 3 da spec `agent`).
2. **Agentes no mapa:** por padrão só os em atividade (`AVAILABLE` + `ON_OCCURRENCE`). O filtro permite ver os demais.
3. **Manutenção atrasada:** `maintenanceNeed = SCHEDULED`, `nextMaintenanceDate` anterior a hoje (fuso `America/Sao_Paulo`) e situação diferente de `DEACTIVATED`. Equipamento fora de uso não tem manutenção cobrada; isso mantém o indicador igual ao número de alertas de atraso (ajuste feito no plano, em 2026-10-26).
4. **Agentes em atividade:** `AVAILABLE` + `ON_OCCURRENCE` (regra 6 da spec `agent`).
5. **Alertas**, calculados a cada consulta e ordenados por severidade e depois pelo mais antigo:

   | Tipo | Severidade | Quando | Desde |
   |---|---|---|---|
   | `EQUIPMENT_MALFUNCTION` | alta | equipamento em `MALFUNCTION` | última mudança para `MALFUNCTION` no histórico |
   | `EQUIPMENT_STOLEN` | alta | equipamento em `STOLEN` | última mudança para `STOLEN` |
   | `REGION_WITHOUT_ACTIVE_AGENT` | alta | região sem nenhum agente em atividade com base nela | — |
   | `MAINTENANCE_OVERDUE` | média | regra 3 | `nextMaintenanceDate` |

   Equipamentos `DEACTIVATED` não geram alerta.
6. **Telemetria simulada** (só no perfil `demo`, ligável por `app.telemetry.enabled`):
   - a cada 10 s, cada agente em atividade dá um passo aleatório de até ~150 m;
   - o agente nunca se afasta mais de ~1,5 km da base;
   - atualiza `positionUpdatedAt`;
   - não grava histórico, porque posição não é mudança de situação;
   - agente fora de serviço não se move.

## API

Todas as rotas são `GET`, para os dois perfis (a regra atual do `SecurityConfig` já cobre). Erros em Problem Details, como na Parte 1.

| Rota | Devolve |
|---|---|
| `GET /api/operations/map?equipmentType&equipmentStatus&agentStatus&region&agency` | `{ equipments: [...], agents: [...] }` em formato leve para os marcadores |
| `GET /api/operations/summary` | indicadores gerais e por região |
| `GET /api/operations/alerts?page&size` | alertas paginados (máx. 100), na ordem da regra 5 |

**Marcador de equipamento:** `id`, `type`, `serialNumber`, `status`, `maintenanceNeed`, `latitude`, `longitude`, `region`.

**Marcador de agente:** `id`, `name`, `agentType`, `status`, `latitude`, `longitude`, `positionSource` (`CURRENT` | `BASE`), `positionUpdatedAt`, `vehiclePlate`.

**Resumo:**

```json
{
  "equipments": { "total": 150, "byStatus": { "OPERATIONAL": 134, "MALFUNCTION": 9, ... },
                  "immediateMaintenance": 10, "overdueMaintenance": 3 },
  "agents": { "total": 40, "active": 17, "byStatus": { "AVAILABLE": 11, ... } },
  "regions": [ { "region": "NORTH", "equipmentsInMalfunction": 4, "activeAgents": 2 }, ... ],
  "generatedAt": "2026-10-27T13:00:00Z"
}
```

**Alerta:** `kind`, `severity` (`HIGH` | `MEDIUM`), `message` (em português), `region`, `equipmentId` ou nulo, `since` ou nulo.

> **Exceção à regra "listagem sempre paginada"** (ver Questões abertas): o `/map` devolve todos os pontos filtrados de uma vez, porque um mapa paginado não faz sentido. Há um teto fixo de 2.000 pontos por camada, e a resposta traz `truncated: true` se o teto for atingido. Hoje são cerca de 190 pontos.

## Front

- **Página "Central" (`/central`):** vira o **primeiro item do menu e a tela inicial** depois do login.
- **Mapa** (Leaflet, já instalado):
  - equipamento: **ícone por tipo** (radar, câmera, balança, veículo, patinete, cancela/totem/outro) e **cor por situação**;
  - agente: marcador próprio (forma diferente do equipamento) com a cor da situação do agente;
  - **detalhe ao clicar:** popup com resumo e link para o detalhe (`/equipamentos/{id}`, `/agentes/{id}`);
  - legenda das cores e dos ícones.
- **Filtros:** camadas (equipamentos / agentes), tipo de equipamento, situação, região e órgão. Ficam na URL, como nas listas.
- **Indicadores:** cartões no topo (com falha, manutenção imediata, manutenção atrasada, agentes em atividade) e uma tabela por região.
- **Alertas:** lista lateral (embaixo, no celular) com severidade, mensagem, tempo decorrido e link.
- **Atualização:** a cada 10 s o mapa, os indicadores e os alertas são recarregados. A atualização pausa com a aba oculta e mostra "atualizado há X s". Falhas de rede mantêm os últimos dados com aviso.
- **Celular:** o mapa ocupa a largura toda, e indicadores e alertas ficam abaixo dele, sem rolagem horizontal.

## Comandos

- Tudo: `docker compose up --build` → http://localhost:3000/central
- Back (pasta `backend/`): `./mvnw test` · `./mvnw test -Dtest=OperationsServiceTest`
- Front (pasta `frontend/`): `npm run lint` · `npm test` · `npm run build`

## Estrutura

- **Back:** `br.com.dsin.challengefogo.operations`
  - `web/OperationsController`
  - `application/OperationsService`, `AlertService` e DTOs em `application/dto` (`MapDtoResponse`, `SummaryDtoResponse`, `AlertDtoResponse`)
  - `application/TelemetrySimulator` (`@Scheduled`, `@Profile("demo")`)
  - consultas de agregação nos repositories de `equipment` e `agent` (JPQL ou SQL nativo), sem entidade nova
- **Front:**
  - `src/lib/api/operations.ts`
  - `src/pages/OperationsPage.tsx`
  - `src/components/OperationsMap.tsx`, `MapLegend.tsx`, `SummaryCards.tsx`, `RegionTable.tsx`, `AlertList.tsx`
  - `src/lib/mapIcons.ts` (ícone por tipo, cor por situação)
  - `src/lib/usePolling.ts`

## Estilo de código

O mesmo da Parte 1 (`AGENTS.md`): records como DTO, construtor recebendo a projeção, injeção por construtor, `@Transactional(readOnly = true)` nas leituras, `Clock` injetado para "hoje" e "desde". Exemplo esperado:

```java
@Transactional(readOnly = true)
public SummaryDtoResponse summary() {
    LocalDate today = LocalDate.now(clock.withZone(CITY_ZONE));
    Map<EquipmentStatus, Long> byStatus = equipmentRepository.countByStatus();
    long overdue = equipmentRepository.countOverdueMaintenance(today);
    ...
    return new SummaryDtoResponse(..., clock.instant());
}
```

## Testes

- **Unitário (service, repositories mockados, `Clock` fixo):**
  - regras 3 e 5: a ordem e as severidades dos alertas, `DEACTIVATED` não alerta, região sem agente alerta;
  - regra 1: escolha da posição;
  - telemetria (regra 6): não passa de 1,5 km da base e ignora agente fora de serviço.
- **Integração (Testcontainers):** as consultas de agregação (contagem por situação, atraso de manutenção, por região) batem com dados de teste conhecidos; o filtro do `/map` funciona.
- **Web (MockMvc):** os 3 GETs para os dois perfis, `401` sem token e o formato JSON.
- **Front (Vitest):**
  - `mapIcons`: tipo vira ícone e situação vira cor;
  - `usePolling` refaz a busca e pausa com a aba oculta;
  - `OperationsPage`: filtros na URL e alertas com link (mapa mockado, como nos outros testes).
- **Navegador:** conferência em celular e desktop, com marcadores, popup, filtros e a posição de um agente mudando após 10 s.

## Limites

- **Sempre:** testes e build antes de dizer que terminou; projeto subindo com `docker compose up --build`; conferir no navegador.
- **Perguntar antes:**
  - qualquer tabela ou migration nova (a spec não prevê nenhuma);
  - dependência nova, por exemplo biblioteca de cluster de marcadores (`leaflet.markercluster`): fora da spec;
  - mudar contrato das rotas da Parte 1.
- **Nunca:** gravar histórico de situação por causa da telemetria; ligar a telemetria fora do perfil `demo` por padrão; ações de escrita na central.

## Critérios de aceite

- [ ] Ao entrar, o usuário cai em `/central` e vê no mapa os 150 equipamentos e os agentes em atividade, com ícone por tipo e cor por situação.
- [ ] Clicar num marcador abre um popup com o resumo e o link para o detalhe.
- [ ] O filtro `equipmentStatus=MALFUNCTION&region=NORTH` mostra só esses equipamentos, e a URL guarda o filtro.
- [ ] Os indicadores batem com o banco (conferidos por SQL): com falha, manutenção atrasada e agentes em atividade, no total e por região.
- [ ] Mudar um equipamento para `MALFUNCTION` no detalhe faz, em até 10 s na central, o marcador ficar vermelho, o indicador subir e surgir um alerta `EQUIPMENT_MALFUNCTION` com "desde" correto.
- [ ] Com a telemetria ligada, a posição de um agente em atividade muda entre duas atualizações, sem nova linha em `agent_status_history`.
- [ ] O operador vê a central inteira, e sem token a resposta é `401`.
- [ ] Na largura de celular (390 px) não há rolagem horizontal, e o mapa, os indicadores e os alertas são usáveis.

## Fora do escopo

Ocorrências e despacho; alertas persistidos e confirmação; SSE ou WebSocket; agrupamento de marcadores; posição de viatura independente do agente; previsão e recomendações (Parte 3).

## Questões abertas

Nenhuma. Resolvidas na aprovação (2026-10-26):
1. **`/map` sem paginação**, com teto fixo de 2.000 pontos por camada e `truncated`: exceção aprovada à regra de listagem paginada.
2. **Tela inicial pós-login:** `/central`, com o menu "Central · Equipamentos · Agentes".
