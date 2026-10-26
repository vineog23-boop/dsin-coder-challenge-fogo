# Tarefas: equipment

Spec: `spec.md`. Plano: `.specs/plan-parte-1.md`.

## T8: Schema e entidades (herança JOINED)

Migration com `equipment`, `radar`, `camera`, `truck_scale`, `vehicle`, `scooter`, `equipment_status_history`, `equipment_maintenance` (FKs, `unique` em `serial_number` e `license_plate`, índices em `status`, `type`, `region`). Enums, `EquipmentEntity` (`@Inheritance(JOINED)`), 5 subclasses, `EquipmentRepository`.

- [x] Aceite: teste de integração (Testcontainers) salva e relê um equipamento de cada subclasse e um `AUTOMATED_GATE` só na tabela base.
- [x] Aceite: `ddl-auto=validate` sobe sem erro.
- Verificação: `./mvnw test -Dtest=EquipmentRepositoryTest`, `./mvnw test`. (Sufixo `Test`, não `IT`: o surefire padrão não roda `*IT`.)
- Arquivos: `V2__create_equipment.sql`, `equipment/domain/*` (enums), `equipment/persistence/*Entity.java`, `EquipmentRepository.java`, teste.
- Depende de: T2.

## T9: Criar equipamento

`EquipmentDtoRequest` (comuns + blocos opcionais), `EquipmentDtoResponse`, `EquipmentService.create`, `POST /api/equipments`.

- [ ] Aceite: regra 1 da spec (bloco ↔ tipo, `speedLimitKmh` em radar `SPEED`) → `400` com o campo apontado.
- [ ] Aceite: serial ou placa duplicada → `409`; criação válida → `201` com `Location`.
- Verificação: unitário do service (TDD, regras 1, 2, 6); MockMvc `201`/`400`/`409`; `./mvnw test`.
- Arquivos: `equipment/web/EquipmentController.java`, DTOs, `equipment/application/EquipmentService.java`, `EquipmentTypeValidator.java`, testes.
- Depende de: T8, T5.

## T10: Buscar e listar

`GET /api/equipments/{id}` e `GET /api/equipments` com filtros da spec (`Specification`), `search` em serial/marca/modelo/placa, página máxima 100.

- [ ] Aceite: id inexistente → `404` (`EquipmentNotFoundException`).
- [ ] Aceite: filtros combinados retornam só os correspondentes, paginados (integração com dados de teste).
- Verificação: `EquipmentSpecificationsTest`, MockMvc; `./mvnw test`.
- Arquivos: `EquipmentSpecifications.java`, service, controller, `EquipmentNotFoundException.java`, testes.
- Depende de: T9.

## T11: Atualizar equipamento

`PUT /api/equipments/{id}` via `equipment.update(...)`; não altera `type` nem `status`.

- [ ] Aceite: mudar dados comuns e do bloco funciona; `type` diferente do atual → `422`.
- Verificação: unitário + MockMvc; `./mvnw test`.
- Arquivos: entidades (método `update`), service, controller, testes.
- Depende de: T10.

## T12: Mudar situação e histórico

`PATCH /api/equipments/{id}/status`, `equipment.changeStatus(...)` grava em `equipment_status_history` com o usuário do token; `GET /api/equipments/{id}/history` (situações + manutenções, mais recente primeiro).

- [ ] Aceite: `MALFUNCTION` → `maintenanceNeed = IMMEDIATE` e linha no histórico; mesma situação → `422`.
- [ ] Aceite: histórico retorna os eventos ordenados.
- Verificação: unitário (TDD, regras 3 e 4); MockMvc; `./mvnw test`.
- Arquivos: `EquipmentStatusHistoryEntity.java` + repository, service, controller, DTO de histórico, testes.
- Depende de: T10.

## T13: Registrar manutenção

`POST /api/equipments/{id}/maintenances`.

- [ ] Aceite: em `MALFUNCTION`/`UNDER_MAINTENANCE` volta a `OPERATIONAL` com histórico; com intervalo, recalcula `nextMaintenanceDate` e `SCHEDULED`; sem intervalo, `NONE`.
- Verificação: unitário (TDD, regra 5); MockMvc; `./mvnw test`.
- Arquivos: `EquipmentMaintenanceEntity.java` + repository, service, controller, DTO, testes.
- Depende de: T12.

## Checkpoint C
- [ ] `./mvnw test` verde; todos os critérios de aceite da API de equipamento conferidos via Swagger.

## T14: Front: lista de equipamentos

`src/lib/api/equipments.ts` (tipos + chamadas), página com tabela paginada, filtros e busca, rótulos em português, estados de carregamento/erro/vazio.

- [ ] Aceite: filtros e paginação refletem na URL e na API.
- Verificação: `npm run lint`, `npm test`, `npm run build`; navegador (celular e desktop).
- Arquivos: `src/lib/api/equipments.ts`, `src/lib/labels.ts`, `src/pages/EquipmentListPage.tsx`, `src/components/EquipmentFilters.tsx`, teste.
- Depende de: T7, T10.

## T15: Front: formulário dinâmico

Criar/editar; campos do bloco mudam conforme o tipo; CEP via ViaCEP (falha não bloqueia); mini-mapa Leaflet para escolher a coordenada; erros da API ligados aos campos.

- [ ] Aceite: trocar o tipo troca os campos; erro `400` da API aparece no campo certo.
- [ ] Aceite: CEP válido preenche rua, bairro e cidade.
- Verificação: lint, teste do formulário (troca de tipo), build; navegador.
- Arquivos: `src/pages/EquipmentFormPage.tsx`, `src/components/TypeSpecificFields.tsx`, `src/components/LocationPicker.tsx`, `src/lib/api/viaCep.ts`, teste.
- Depende de: T14, T11.

## T16: Front: detalhe e ações

Detalhe com características, localização (mapa pequeno), histórico em linha do tempo; mudar situação e registrar manutenção (só gestor).

- [ ] Aceite: ações atualizam o detalhe e o histórico sem recarregar a página; operador não vê as ações.
- Verificação: lint, test, build; navegador com os dois perfis.
- Arquivos: `src/pages/EquipmentDetailPage.tsx`, `src/components/StatusHistory.tsx`, `src/components/StatusChangeDialog.tsx`, `src/components/MaintenanceDialog.tsx`.
- Depende de: T15, T13.

## T17: Seed de equipamentos

`shared/demo/DemoEquipmentSeeder`: ~150 equipamentos em Presidente Prudente, todos os tipos e regiões, `Random(42)`; ~6 meses de histórico com mais falhas em certas regiões e horários (documentar o padrão em comentário, para a Parte 3 validar).

- [ ] Aceite: banco vazio + perfil `demo` → ~150 equipamentos; segunda subida não duplica.
- [ ] Aceite: duas subidas do zero geram os mesmos dados.
- Verificação: teste de integração do seeder (contagem e idempotência); `docker compose down -v && docker compose up --build`.
- Arquivos: `shared/demo/DemoEquipmentSeeder.java`, `shared/demo/PrudenteGeo.java` (ruas, bairros, CEPs, área), teste.
- Depende de: T13.

## Checkpoint D
- [ ] Cadastro de equipamento completo no navegador, com seed visível; critérios de aceite da spec `equipment` marcados.
