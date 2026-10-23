# Tarefas: auth

Spec: `spec.md`. Plano: `.specs/plan-parte-1.md`.

## T5: Segurança e serviço de token

`auth/application/TokenService` (emite e valida JWT HS256 com claim `role`, validade de 8 h, segredo de `JWT_SECRET`). `SecurityConfig` sem sessão: `/api/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**` e health públicos; `GET` para os dois perfis; escrita só `MANAGER`, exceto `PATCH /api/agents/*/position`. `401`/`403` em Problem Details.

- [x] Aceite: token expirado ou com assinatura errada é recusado (teste unitário).
- [x] Aceite: `GET` sem token → `401`; `POST` com token `OPERATOR` → `403`; com `MANAGER` passa (MockMvc com controller de teste).
- Verificação: `./mvnw test -Dtest='TokenServiceTest,SecurityConfigTest'`, depois `./mvnw test`.
- Arquivos: `auth/application/TokenService.java`, `shared/security/SecurityConfig.java`, `shared/security/ProblemAuthenticationEntryPoint.java`, testes.
- Depende de: T2.

## T6: Usuários, login e usuários demo

Migration `app_user` (`id`, `username` único, `password_hash`, `role`). `UserEntity`, `UserRepository`, `AuthService.login` (BCrypt), `AuthController` `POST /api/auth/login`. Seeder demo cria `gestor` e `operador` se não existirem.

- [ ] Aceite: login correto devolve `accessToken`, `expiresIn`, `role`; usuário inexistente e senha errada devolvem o mesmo `401`.
- [ ] Aceite: com o perfil `demo`, os dois usuários existem após subir a aplicação.
- Verificação: unitário de `AuthService` (mock do repository); MockMvc do controller; `./mvnw test`; `curl` no login com o compose.
- Arquivos: `V2__create_app_user.sql`, `auth/persistence/UserEntity.java`, `auth/persistence/UserRepository.java`, `auth/application/AuthService.java`, `auth/web/AuthController.java` (+ DTOs), `shared/demo/DemoUserSeeder.java`.
- Depende de: T5.

## T7: Front de login

Página de login; `src/lib/api/auth.ts`; contexto de sessão (token em memória + `sessionStorage`, perfil); rotas protegidas; `401` → volta ao login; logout; helper `canEdit` para esconder ações do operador.

- [ ] Aceite: login com `gestor` e `operador` funciona; credencial errada mostra mensagem.
- [ ] Aceite: recarregar a página mantém a sessão; logout limpa.
- Verificação: `npm run lint`, `npm test` (teste do contexto de sessão), `npm run build`; navegador.
- Arquivos: `src/pages/LoginPage.tsx`, `src/lib/api/auth.ts`, `src/lib/session/SessionContext.tsx`, `src/components/ProtectedRoute.tsx`, teste.
- Depende de: T3, T6.

## Checkpoint B
- [ ] Login funciona no navegador com os dois perfis via `docker compose up --build`.
