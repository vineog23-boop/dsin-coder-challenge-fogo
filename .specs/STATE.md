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

Agentes configurados com `/iniciar`. Specs da Parte 1 aprovadas. Plano em `.specs/plan-parte-1.md` (T1–T22, tarefas nos `tasks.md` de `setup`, `auth`, `equipment`, `agent`), aprovado. T1–T4 e Checkpoint A concluídos (backend Boot 4.1.1 + Postgres 17 na porta 5433; `GlobalExceptionHandler` com Problem Details; front Vite + React 19 + React Router, rotas `/equipamentos` e `/agentes`, cliente `api()` em `src/lib/api/client.ts` que lança `ApiError` com o Problem Details). T4: `docker compose up --build` sobe db, backend (perfil `demo`, sem porta no host) e front em `http://localhost:3000`; o nginx repassa `/api/` ao back e mapeia `/api/actuator/health` → `/actuator/health`. Imagem do back pula os testes (Testcontainers precisa de Docker). Próximo passo: `/fazer` T5.

**Navegador:** não há Google Chrome na máquina (o MCP chrome-devtools falha); a conferência de front usou o Chromium do Playwright via script no scratchpad.

**Atenção no T5:** o `GlobalExceptionHandler` tem `@ExceptionHandler(Exception.class)` → 500. `AccessDeniedException`/`AuthenticationException` lançadas dentro do controller (ex.: `@PreAuthorize`) cairiam nele; tratar as duas explicitamente (403/401 em Problem Details) ao configurar a segurança.
