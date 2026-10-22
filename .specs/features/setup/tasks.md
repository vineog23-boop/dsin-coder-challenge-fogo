# Tarefas: setup (infraestrutura)

Plano: `.specs/plan-parte-1.md`.

## T1: Scaffold do backend ✅ (22/10/2026)

Projeto Spring Boot 4.1.1 em `backend/` (Maven Wrapper, Java 21, pacote `br.com.dsin.challengefogo`) com as dependências aprovadas. `docker-compose.yml` na raiz com o serviço `db` (PostgreSQL 17, porta **5433** no host, porque a 5432 é usada por outro projeto). `application.properties` (mesmo formato do Medflow) com `ddl-auto=validate` e Flyway ligados.

- [x] Aceite: `./mvnw test` roda um teste de contexto que sobe PostgreSQL via Testcontainers (`@ServiceConnection`).
- [x] Aceite: a aplicação conecta no `db` do compose e o Flyway roda (0 migrations); `/actuator/health` → 200.
- Verificação: `docker compose up -d db`, `./mvnw test`, `./mvnw -q package`.
- Arquivos: `backend/pom.xml`, `backend/mvnw*`, `backend/src/main/resources/application.properties`, `ChallengefogoApplication.java`, `ChallengefogoApplicationTests.java`, `TestcontainersConfiguration.java`, `docker-compose.yml`, `.gitignore`.
- Depende de: nada.

## T2: Problem Details e tipos compartilhados

`shared/web/GlobalExceptionHandler`, `shared/web/ProblemTypes` (`urn:fogo:problem:<erro>`), exceções base (`NotFoundException`, `ConflictException`, `BusinessRuleException`), `shared/domain/Location` (`@Embeddable`), enums `Agency` e `Region`.

- [ ] Aceite: `MethodArgumentNotValidException` → `400` com a lista de campos inválidos; `NotFound` → `404`; `Conflict` → `409`; `BusinessRule` → `422`, todos com `type` URN.
- [ ] Aceite: erro inesperado → `500` sem stack trace no corpo.
- Verificação: teste MockMvc com um controller de teste (`@WebMvcTest`) cobrindo os 5 casos; `./mvnw test`.
- Arquivos: `shared/web/*`, `shared/domain/*`, teste do handler.
- Depende de: T1.

## T3: Scaffold do front

`frontend/` com Vite + React + TypeScript, ESLint, Vitest, React Router. Layout com menu (Equipamentos, Agentes), páginas vazias e `src/lib/api/client.ts` (base `/api`, JSON, erro em Problem Details tipado). Proxy do Vite para `localhost:8080`.

- [ ] Aceite: `npm run lint`, `npm test` e `npm run build` passam.
- [ ] Aceite: `npm run dev` abre o layout, e a navegação entre as páginas funciona (conferido no navegador em largura de celular e desktop).
- Verificação: os três comandos + DevTools.
- Arquivos: `frontend/package.json`, `vite.config.ts`, `src/main.tsx`, `src/App.tsx`, `src/lib/api/client.ts`, `src/lib/api/client.test.ts`.
- Depende de: nada.

## T4: Projeto sobe com um comando

`backend/Dockerfile` (multi-stage, Temurin 21), `frontend/Dockerfile` (build + nginx com proxy de `/api`), compose com `db`, `backend` (perfil `demo`, espera o banco saudável) e `frontend`; `.env.example` com `JWT_SECRET` e as credenciais do banco.

- [ ] Aceite: em clone limpo, `docker compose up --build` deixa o front em `http://localhost:3000`, e `/api/actuator/health` (ou endpoint equivalente) responde `UP` via nginx.
- [ ] Aceite: funciona sem `.env` (valores padrão de desenvolvimento no compose), e `.env` está no `.gitignore`.
- Verificação: `docker compose down -v && docker compose up --build`; `curl`.
- Arquivos: `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, `docker-compose.yml`, `.env.example`.
- Depende de: T1, T3.

## Checkpoint A
- [ ] `./mvnw test`, `npm test`, `npm run build` verdes; `docker compose up --build` sobe tudo do zero.

## T22: README de entrega e verificação final

README na raiz: o que é, como rodar (um comando), credenciais demo, mapa das funcionalidades, decisões técnicas, **IAs usadas e como foram usadas** (exigência do desafio), roteiro do vídeo.

- [ ] Aceite: seguindo só o README, em clone limpo, o sistema sobe e o login demo funciona.
- [ ] Aceite: todos os critérios de aceite das specs `auth`, `equipment` e `agent` conferidos e marcados.
- Verificação: `docker compose down -v && docker compose up --build` + roteiro manual.
- Arquivos: `README.md`.
- Depende de: T21.
