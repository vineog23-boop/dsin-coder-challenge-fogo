# Spec: equipment

Status: aguardando aprovação

## Objetivo

Catalogar os equipamentos IoT de trânsito e segurança da cidade com localização, situação e manutenção, guardando o **histórico** de cada mudança. Esse cadastro alimenta o mapa da Parte 2 e as previsões da Parte 3.

## Modelo

### Campos comuns (`equipment`)

| Campo | Regra |
|---|---|
| `id` | UUID |
| `type` | enum `EquipmentType` (abaixo), obrigatório, **não muda depois de criado** |
| `serialNumber` | obrigatório, único |
| `brand`, `model` | obrigatórios |
| `agency` | enum `Agency`: `MUNICIPAL_TRAFFIC`, `MUNICIPAL_GUARD`, `MILITARY_POLICE`, `PRIVATE_OPERATOR` |
| `status` | enum `EquipmentStatus`: `NOT_INSTALLED`, `OPERATIONAL`, `MALFUNCTION`, `UNDER_MAINTENANCE`, `DEACTIVATED`, `STOLEN` |
| `maintenanceNeed` | enum: `NONE`, `IMMEDIATE`, `SCHEDULED` |
| `nextMaintenanceDate` | `LocalDate`, obrigatória quando `SCHEDULED` |
| `maintenanceIntervalDays` | opcional; manutenção periódica |
| `location` | embutido (abaixo) |
| `createdAt`, `updatedAt` | `Instant` |

### Localização (embutida, reaproveitada pelo agente)

`zipCode` (8 dígitos), `street`, `reference` (ex.: "número 181", "oposto ao 181", "KM 203", "S/N", "em frente ao Mercado do Tio"), `neighborhood`, `city`, `state` (UF, 2 letras), `region` (enum `CENTER`, `NORTH`, `SOUTH`, `EAST`, `WEST`), `latitude`, `longitude` (`BigDecimal`, 6 casas, coluna `numeric(9,6)`).

### Tipos e tabelas filhas (herança JPA `JOINED`)

| Tabela filha | Tipos (`EquipmentType`) | Campos próprios |
|---|---|---|
| `radar` | `RADAR` | `modality` (`SPEED`, `RED_LIGHT`, `ILLEGAL_TURN`, `ILLEGAL_U_TURN`, `BUS_LANE`); `speedLimitKmh` obrigatório se `SPEED`; `lanes` |
| `camera` | `SURVEILLANCE_CAMERA` | `nightVision`, `resolution` (`HD_720`, `FULL_HD_1080`, `QHD_1440`, `UHD_4K`), `color`, `ptz` (gira/zoom) |
| `truck_scale` | `TRUCK_SCALE` | `maxCapacityTons`, `weighInMotion` |
| `vehicle` | `TAXI`, `MOTO_TAXI`, `SCHOOL_TRANSPORT`, `PARKING_ENFORCEMENT_VEHICLE` (autônomo), `TRAFFIC_AGENT_MOTORCYCLE`, `MUNICIPAL_GUARD_VEHICLE`, `MILITARY_POLICE_VEHICLE` | `licensePlate` (única, padrão Mercosul ou antigo), `passengerCapacity`, `autonomous` |
| — (só `equipment`) | `AUTOMATED_GATE`, `PARKING_KIOSK`, `OTHER` | — |
| `scooter` | `ELECTRIC_SCOOTER` | `ownership` (`PUBLIC`, `SHARED`), `operatorName` (se `SHARED`), `batteryLevel` (0–100) |

Para veículos, a localização é a da **base** (garagem/ponto).

### Histórico

- `equipment_status_history`: `equipmentId`, `fromStatus`, `toStatus`, `changedAt`, `changedBy`, `note`.
- `equipment_maintenance`: `equipmentId`, `kind` (`CORRECTIVE`, `PREVENTIVE`), `performedAt`, `description`, `performedBy`.

## Regras de negócio

1. O bloco de características deve corresponder ao tipo: `RADAR` exige `radar` e recusa `camera`; tipo sem tabela filha recusa qualquer bloco. → `400` com o campo apontado.
2. `serialNumber` e `licensePlate` duplicados → `409`.
3. Mudar para `MALFUNCTION` define `maintenanceNeed = IMMEDIATE` automaticamente.
4. Toda mudança de situação grava uma linha no histórico; mudar para a mesma situação → `422`.
5. Registrar manutenção:
   - grava em `equipment_maintenance`;
   - se a situação era `MALFUNCTION` ou `UNDER_MAINTENANCE`, volta a `OPERATIONAL` (com linha no histórico);
   - se há `maintenanceIntervalDays`, `nextMaintenanceDate = performedAt + intervalo` e `maintenanceNeed = SCHEDULED`; senão `NONE`.
6. `SCHEDULED` sem `nextMaintenanceDate` → `400`. Data no passado é permitida (manutenção atrasada é dado útil para a Parte 2).
7. Sem exclusão física: equipamento sai de uso com a situação `DEACTIVATED`, preservando o histórico.

## API

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/equipments` | gestor |
| `GET` | `/api/equipments?type&status&region&neighborhood&agency&maintenanceNeed&search&page&size&sort` | ambos |
| `GET` | `/api/equipments/{id}` | ambos |
| `PUT` | `/api/equipments/{id}` (não altera `type` nem `status`) | gestor |
| `PATCH` | `/api/equipments/{id}/status` `{ status, note }` | gestor |
| `POST` | `/api/equipments/{id}/maintenances` `{ kind, performedAt, description }` | gestor |
| `GET` | `/api/equipments/{id}/history` (situações + manutenções, mais recente primeiro) | ambos |

`search` busca em número de série, marca, modelo e placa. Tamanho máximo de página: 100. A resposta inclui o bloco de características do tipo.

## Front

- Lista paginada com filtros (tipo, situação, região, órgão, manutenção) e busca.
- Formulário: os campos de características mudam conforme o tipo escolhido; CEP preenche rua, bairro e cidade via ViaCEP (se falhar, preenchimento manual continua); latitude e longitude escolhidas clicando num mini-mapa.
- Detalhe com histórico em linha do tempo; ações de mudar situação e registrar manutenção (só gestor).
- Rótulos em português para todos os enums.

## Seed (perfil `demo`, ativo no docker compose)

- Roda só com o banco vazio. `Random` com semente fixa: os dados são iguais a cada subida.
- ~150 equipamentos em Presidente Prudente/SP, com coordenadas dentro da área urbana, distribuídos nas 5 regiões e em todos os tipos.
- ~6 meses de histórico de situação e manutenção, com mais falhas em algumas regiões e horários, para a Parte 3 ter padrão a encontrar.

## Critérios de aceite

- [x] Criar um equipamento de cada grupo (radar, câmera, balança, veículo, patinete, cancela) e lê-lo de volta com suas características.
- [x] `RADAR` com bloco `camera` → `400`; radar `SPEED` sem `speedLimitKmh` → `400`.
- [x] Número de série duplicado → `409`.
- [x] Mudar para `MALFUNCTION` gera histórico e `maintenanceNeed = IMMEDIATE`.
- [x] Registrar manutenção em equipamento com falha o devolve a `OPERATIONAL` e recalcula a próxima data.
- [x] Filtros combinados (ex.: `type=RADAR&region=NORTH&status=MALFUNCTION`) retornam só os correspondentes, paginados.
- [x] Após `docker compose up --build`, o seed aparece em `GET /api/equipments`.
- [x] Formulário do front troca os campos ao trocar o tipo e mostra erro de validação vindo da API.

## Testes

- Unitário (service, mock do repository): regras 1 a 6.
- Integração (Testcontainers, PostgreSQL): herança `JOINED` salva e lê cada subtipo; filtros combinados; unicidade.
- Web (MockMvc): status `400`, `404`, `409`, `422` em Problem Details.

## Fora do escopo

Telemetria, mapa do painel e alertas (Parte 2); previsões (Parte 3); importação em lote.
