# Spec: auth

Status: aguardando aprovação

## Objetivo

Proteger a API e o dashboard com login, separando quem **altera** dados (gestor) de quem só **acompanha** (operador). Requisito da comissão: o avaliador precisa entrar sem atrito, então as credenciais de demonstração ficam no README.

## Regras

- `POST /api/auth/login` com `{ "username", "password" }` devolve `{ "accessToken", "expiresIn", "role" }`.
- Token JWT assinado (HS256), validade de 8 horas, sem refresh token.
- Perfis:
  - `MANAGER` (gestor): leitura e escrita em tudo.
  - `OPERATOR` (operador): somente leitura (`GET`), mais `PATCH` de posição de agente (usado pela telemetria na Parte 2).
- Senhas com BCrypt. Usuários `gestor` e `operador` criados pelo seed de demonstração.
- Segredo do JWT por variável de ambiente `JWT_SECRET`; `.env.example` versionado, `.env` nunca.
- Sem token → `401`; perfil sem permissão → `403`; ambos em Problem Details.
- Sem cadastro de usuários pela API nesta fase.

## Front

- Tela de login; token guardado em memória + `sessionStorage`; logout limpa.
- Botões de criar, editar e mudar situação não aparecem para o operador.
- `401` em qualquer chamada leva de volta ao login.

## Critérios de aceite

- [ ] Login válido devolve token; inválido devolve `401` sem dizer se o erro foi usuário ou senha.
- [ ] Operador recebe `403` em `POST`/`PUT` de equipamento e agente.
- [ ] Gestor consegue criar equipamento.
- [ ] Endpoints sem token devolvem `401`; `/api/auth/login` e a documentação OpenAPI são públicos.

## Testes

- MockMvc: login ok, login inválido, `401` sem token, `403` operador escrevendo, `200` gestor.
- Unitário: geração e validação do token (expirado, assinatura inválida).

## Dependências novas (aprovadas na entrevista)

- `spring-boot-starter-security` e uma biblioteca JWT (`spring-boot-starter-oauth2-resource-server`, que já valida JWT com HS256 sem lib extra).
