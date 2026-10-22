# Spec: agent

Status: aguardando aprovação

## Objetivo

Cadastrar os agentes que atuam na operação (agentes de trânsito, guardas municipais e policiais militares) para que apareçam no mapa da Parte 2 com situação e posição, e para que a Parte 3 possa sugerir reforço por região.

## Modelo (`agent`)

| Campo | Regra |
|---|---|
| `id` | UUID |
| `name` | obrigatório |
| `registrationNumber` | matrícula, obrigatória, única |
| `agentType` | `TRAFFIC_AGENT`, `MUNICIPAL_GUARD`, `MILITARY_POLICE` |
| `agency` | derivado do tipo: `MUNICIPAL_TRAFFIC`, `MUNICIPAL_GUARD`, `MILITARY_POLICE` (mesmo enum `Agency` do equipamento) |
| `status` | `AVAILABLE` (em serviço, livre), `ON_OCCURRENCE` (em atendimento), `OFF_DUTY` (fora de serviço), `ON_LEAVE` (afastado) |
| `baseLocation` | mesma localização embutida do equipamento (base/batalhão/posto) |
| `currentLatitude`, `currentLongitude`, `positionUpdatedAt` | opcionais; posição em campo |
| `vehicleId` | opcional; veículo atual (equipamento) |
| `createdAt`, `updatedAt` | `Instant` |

Histórico: `agent_status_history` (`agentId`, `fromStatus`, `toStatus`, `changedAt`, `changedBy`, `note`).

Dados pessoais mínimos: sem CPF, telefone ou endereço residencial.

## Regras de negócio

1. Matrícula duplicada → `409`.
2. Veículo vinculado deve:
   - existir (`404` se não);
   - ser `TRAFFIC_AGENT_MOTORCYCLE`, `MUNICIPAL_GUARD_VEHICLE` ou `MILITARY_POLICE_VEHICLE`;
   - pertencer ao mesmo órgão do agente;
   - não estar `DEACTIVATED` nem `STOLEN`.
   Violação → `422`. Um veículo pode ter mais de um agente (viatura com dupla).
3. Posição exibida no mapa: a posição atual, se houver; senão, a base. (A posição do veículo entra com a telemetria da Parte 2.)
4. Mudança de situação grava histórico; mesma situação → `422`.
5. Agente `OFF_DUTY` ou `ON_LEAVE` perde o vínculo com o veículo automaticamente.
6. "Agentes em atividade" (indicador da Parte 2) = `AVAILABLE` + `ON_OCCURRENCE`.
7. Sem exclusão física: desligamento é `ON_LEAVE`.

## API

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/agents` | gestor |
| `GET` | `/api/agents?agentType&status&agency&region&search&page&size&sort` | ambos |
| `GET` | `/api/agents/{id}` (inclui resumo do veículo) | ambos |
| `PUT` | `/api/agents/{id}` (não altera `status`) | gestor |
| `PATCH` | `/api/agents/{id}/status` `{ status, note }` | gestor |
| `PATCH` | `/api/agents/{id}/position` `{ latitude, longitude }` | ambos |
| `GET` | `/api/agents/{id}/history` | ambos |

`search` busca em nome e matrícula. `region` filtra pela região da base.

## Front

- Lista paginada com filtros e busca; formulário com seletor de veículo que só oferece veículos compatíveis com o órgão.
- Detalhe com histórico de situação; ações só para gestor.

## Seed (perfil `demo`)

- ~40 agentes em Presidente Prudente/SP, distribuídos entre os três órgãos e as 5 regiões; parte com veículo; ~6 meses de histórico de situação.
- Nomes fictícios.

## Critérios de aceite

- [ ] Criar agente de cada tipo e lê-lo com o órgão derivado.
- [ ] Vincular viatura da PM a um guarda municipal → `422`.
- [ ] Vincular veículo `STOLEN` → `422`.
- [ ] Mudar para `OFF_DUTY` grava histórico e remove o veículo.
- [ ] Operador consegue atualizar posição e recebe `403` ao mudar situação.
- [ ] Filtro `agency=MUNICIPAL_GUARD&status=AVAILABLE` retorna só os correspondentes.

## Testes

- Unitário (service): regras 2, 4 e 5.
- Integração (Testcontainers): filtros, unicidade, vínculo com `vehicle`.
- Web (MockMvc): `403` do operador, `404`, `409`, `422`.

## Fora do escopo

Escala de turnos, ocorrências (entram na Parte 2 se houver tempo), rastreamento contínuo.
