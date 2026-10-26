# STATE: fogo

Desafio F.O.G.O. (DSIN Coder Challenge, Protocolo PROMETHEUS, etapa bônus). **Prazo: 2026-10-27 23:59.** Objetivo: portfólio.

## Decisions

- **Escopo**: as 3 partes (inventário, central com mapa, previsão).
  - Reason: o enunciado avalia quem resolve mais e melhor.
  - Scope: projeto todo.
- **Arquitetura**: back-end por feature `<feature>/{web,application,domain,persistence}`, pacote `br.com.dsin.challengefogo`.
  - Reason: coesão por assunto; estilo padrão do Vinícius.
  - Scope: `backend/`.
- **Nomes**: DTO `XxxDtoRequest`/`XxxDtoResponse`; entidade JPA com sufixo `Entity`.
  - Reason: elimina a inconsistência que o estilo global deixava em aberto.
- **Stack**: Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway; front React + TypeScript + Vite + Leaflet.
  - Trade-off: front separado exige CORS/compose, em troca de dashboard mais rico.
- **Entrega**: `docker compose up --build` sobe tudo.
  - Reason: o enunciado diz que projeto que roda de primeira recebe nota melhor.
- **Previsão**: regras e estatística primeiro; ML só com aprovação.
- **Características por tipo de equipamento**: herança JPA `JOINED`, com 5 tabelas filhas (radar, camera, truck_scale, vehicle, scooter); tipos sem características próprias ficam só em `equipment`.
  - Reason: tabelas e colunas explícitas, fáceis de explicar; o Vinícius não tem experiência com JSONB.
  - Trade-off: um tipo novo com características exige migration e entidade.
- **Coordenadas**: `BigDecimal`, coluna `numeric(9,6)`.
- **Histórico** de situação e manutenção desde a Parte 1 (insumo da Parte 3).
- **Auth**: JWT, perfis `MANAGER` e `OPERATOR`, usuários de demonstração no seed (credenciais no README).
  - Trade-off: mais atrito na avaliação, compensado pelo README.
- **Seed**: perfil `demo`, Presidente Prudente/SP, semente fixa, só com o banco vazio.
- **Sem exclusão física** de equipamento ou agente; saída de uso por situação.

## Pendências

- Declarar na entrega quais IAs foram usadas e demonstrar entendimento do que foi construído.
- Vídeo curto do sistema rodando vale ponto extra.

## Handoff

Agentes configurados com `/iniciar`. Specs da Parte 1 aprovadas. Plano em `.specs/plan-parte-1.md` (T1–T22, tarefas nos `tasks.md` de `setup`, `auth`, `equipment`, `agent`), aprovado. T1–T4 e Checkpoint A concluídos (backend Boot 4.1.1 + Postgres 17 na porta 5433; `GlobalExceptionHandler` com Problem Details; front Vite + React 19 + React Router, rotas `/equipamentos` e `/agentes`, cliente `api()` em `src/lib/api/client.ts` que lança `ApiError` com o Problem Details). T4: `docker compose up --build` sobe db, backend (perfil `demo`, sem porta no host) e front em `http://localhost:3000`; o nginx repassa `/api/` ao back e mapeia `/api/actuator/health` → `/actuator/health`. Imagem do back pula os testes (Testcontainers precisa de Docker). T5 concluído: `TokenService` (Nimbus do resource server, `Clock` injetado via `ClockConfig`), `SecurityConfig` stateless; 401/403 passam pelo `HandlerExceptionResolver` até o `GlobalExceptionHandler` (que agora trata `AuthenticationException`/`AccessDeniedException`, resolvendo a atenção abaixo). Segredo em `app.jwt.secret` (`JWT_SECRET`, mínimo 32 bytes). T6 concluído: `app_user` (V1), `AuthService.login` com BCrypt (usuário inexistente também compara contra hash fictício, tempo igual), `InvalidCredentialsException` → 401 "Usuário ou senha inválidos.", `DemoUserSeeder` (`gestor`/`gestor123`, `operador`/`operador123`). DTOs ficam em `<feature>/application/dto` para o service devolvê-los sem depender de `web` — seguir esse padrão nas outras features. Migrations renumeradas: equipment passa a ser V2. T7 e Checkpoint B concluídos: `SessionProvider` (`src/lib/session/SessionContext.tsx`) guarda `{token, role, username, expiresAt}` em `sessionStorage` (`fogo.session`) e descarta sessão vencida ao recarregar; `useSession`/`canEdit` ficam em `session.ts` (regra `only-export-components`). O `client.ts` recebe o token por `setAuthToken` e chama o handler de `setUnauthorizedHandler` em `401` só quando havia token (no login, `401` é credencial errada). `ProtectedRoute` manda ao `/login` guardando a rota pedida; cabeçalho mostra usuário/perfil e "Sair". Telas de escrita devem esconder ações com `canEdit(session)`. T8 concluído: `V2__create_equipment.sql` (base + 5 filhas com o mesmo `id` como PK/FK, histórico e manutenção; `CHECK` nos enums e nas regras de `SCHEDULED`, radar `SPEED` e patinete `SHARED`), `EquipmentEntity` concreta `JOINED` sem discriminador (vários tipos em `vehicle`; o tipo de negócio é `type`), subclasses fixam o tipo no construtor (menos `VehicleEntity`), `createdAt/updatedAt` por `@CreationTimestamp/@UpdateTimestamp`. Entidades de histórico/manutenção ficam para T12/T13. Testes de persistência usam sufixo `Test` (surefire padrão não roda `*IT`) e `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `TestcontainersConfiguration`. G1 (T9–T13) e Checkpoint C concluídos: `EquipmentService` (create/findById/search/update/changeStatus/registerMaintenance/history), `EquipmentController` em `/api/equipments`, `EquipmentTypeValidator` (regra 1, 6, radar `SPEED`, patinete `SHARED`) lança `FieldValidationException` → `400` com `errors[{field,message}]` (campo do bloco: `radar`, `radar.speedLimitKmh`, `vehicle`...). `EquipmentType.block()` liga tipo ↔ `CharacteristicsBlock`. `EquipmentSpecifications` (em `application`; placa por subquery). `status` no request só vale na criação (padrão `NOT_INSTALLED`); no `PUT`, `status` ou `type` diferentes → `422`. Histórico mescla situações + manutenções paginado (`HistoryEventDtoResponse`, `eventType` `STATUS_CHANGE`/`MAINTENANCE`). Datas de manutenção convertidas no fuso `America/Sao_Paulo`. `POST /maintenances` → `201` com `MaintenanceDtoResponse`. Páginas no formato `{content, page:{size,number,totalElements,totalPages}}`; limite 100 por `PageableHandlerMethodArgumentResolverCustomizer` no `WebConfig` (a propriedade `max-page-size` é ignorada com `@EnableSpringDataWebSupport`); `sort` inválido → `400`. `LocationDto` em `shared/application/dto` (agente reaproveita). Próximo passo: `/fazer` grupo G2 (T14–T17).

**Execução em grupos** (decidido em 2026-10-26, prazo apertado): G1 = T9–T13 → Checkpoint C; G2 = T14–T17 → Checkpoint D; G3 = T18–T22 → Checkpoint E. Verificação completa no fim do grupo, uma confirmação, um commit por tarefa.

**Mapa visual:** https://claude.ai/artifact/9TcqJGL7jZrTcTn8KNu8kB — ao concluir cada tarefa, ler a versão atual (Artifact `read`) e atualizar no mesmo link: progresso, classes novas, diagramas afetados, trechos de código e testes.

**Navegador:** não há Google Chrome na máquina (o MCP chrome-devtools falha); a conferência de front usou o Chromium do Playwright via script no scratchpad.

**Atenção no T5:** o `GlobalExceptionHandler` tem `@ExceptionHandler(Exception.class)` → 500. `AccessDeniedException`/`AuthenticationException` lançadas dentro do controller (ex.: `@PreAuthorize`) cairiam nele; tratar as duas explicitamente (403/401 em Problem Details) ao configurar a segurança.
