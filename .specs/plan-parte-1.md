# Plano: Parte 1 (auth, equipment, agent)

Specs: `.specs/features/{auth,equipment,agent}/spec.md`. Tarefas: `tasks.md` de cada módulo, mais `.specs/features/setup/tasks.md` (infraestrutura, não é módulo do mapa).

**Meta de calendário:** Parte 1 pronta até o fim de 24/10/2026, deixando 25–27/10 para as Partes 2 e 3.

## Decisões técnicas

- Spring Boot **4.1.1** (mesma versão do Medflow), Java 21, Maven Wrapper. Grupo/pacote `br.com.dsin.challengefogo`.
- Herança `JOINED`: `EquipmentEntity` concreta (cancela, totem, outros) e subclasses `RadarEntity`, `CameraEntity`, `TruckScaleEntity`, `VehicleEntity`, `ScooterEntity`.
- Request de equipamento: campos comuns + blocos opcionais `radar`, `camera`, `truckScale`, `vehicle`, `scooter`; um validador no service exige o bloco do tipo e recusa os demais. Sem polimorfismo do Jackson, para manter o contrato fácil de ler.
- Filtros da listagem com `Specification` do Spring Data (combinação dinâmica sem SQL montado na mão).
- JWT emitido e validado com `spring-boot-starter-oauth2-resource-server` (`NimbusJwtEncoder`/`Decoder`, HS256). O perfil vai na claim `role`.
- Seed em `shared/demo` com `@Profile("demo")`, `ApplicationRunner`, `new Random(42)`. Coordenadas sorteadas na área urbana de Presidente Prudente; a região é definida pelo quadrante em relação ao centro (−22,1207; −51,3925).
- Front: Vite + React 19 + TypeScript, React Router, Leaflet via `react-leaflet`, Vitest + Testing Library.
- Docker: backend em imagem Temurin 21 multi-stage; front servido por nginx, que também faz proxy de `/api` para o backend (sem CORS em produção). Em dev, proxy do Vite.

## Dependências (aprovadas)

| Onde | Dependência | Por quê |
|---|---|---|
| back | starters `webmvc`, `data-jpa`, `validation`, `flyway` (+ `flyway-database-postgresql`), `security`, `oauth2-resource-server`, `actuator`; `postgresql` | stack aprovada; actuator para o health do compose |
| back | starters `*-test` correspondentes, `spring-boot-testcontainers`, `testcontainers-postgresql` | testes (mesmo padrão do Medflow) |
| back | `springdoc-openapi-starter-webmvc-ui` 3.1.1 | Swagger para a comissão testar a API (linha 3.x é a compatível com Boot 4) |
| front | react-router, leaflet, react-leaflet | navegação e mapa |
| front | vitest, @testing-library/react, jsdom | testes |

## Ordem e checkpoints

| # | Tarefa | Arquivo | Tamanho |
|---|---|---|---|
| T1 | Scaffold do backend + Postgres no compose + teste de contexto | setup | M |
| T2 | Tratamento de erro (Problem Details) + tipos compartilhados | setup | S |
| T3 | Scaffold do front (layout, roteamento, cliente da API) | setup | M |
| T4 | `docker compose up --build` sobe banco, API e front | setup | M |
| ✔ | **Checkpoint A**: projeto roda de ponta a ponta vazio | | |
| T5 | Configuração de segurança + serviço de token | auth | M |
| T6 | Usuários, login e seed dos usuários demo | auth | M |
| T7 | Front: login, guarda de rota, logout, 401 | auth | M |
| ✔ | **Checkpoint B**: login funciona no navegador com os dois perfis | | |
| T8 | Schema e entidades de equipamento (herança `JOINED`) | equipment | M |
| T9 | Criar equipamento (validação tipo ↔ bloco, unicidade) | equipment | M |
| T10 | Buscar por id e listar com filtros e paginação | equipment | M |
| T11 | Atualizar equipamento | equipment | S |
| T12 | Mudar situação + histórico | equipment | M |
| T13 | Registrar manutenção | equipment | S |
| ✔ | **Checkpoint C**: API de equipamento completa e testada | | |
| T14 | Front: lista de equipamentos com filtros | equipment | M |
| T15 | Front: formulário dinâmico por tipo (ViaCEP, mini-mapa) | equipment | M |
| T16 | Front: detalhe, histórico e ações | equipment | M |
| T17 | Seed de equipamentos e histórico | equipment | M |
| ✔ | **Checkpoint D**: cadastro de equipamento completo no navegador | | |
| T18 | Agente: schema, criar, buscar, listar, atualizar, regras de veículo | agent | M |
| T19 | Agente: situação, posição, histórico | agent | S |
| T20 | Front de agentes (lista, formulário, detalhe) | agent | M |
| T21 | Seed de agentes | agent | S |
| T22 | README de entrega e verificação final do zero | setup | S |
| ✔ | **Checkpoint E**: todos os critérios de aceite das 3 specs | | |

Paralelizáveis depois do T4: T7 e T14–T16 (front) podem andar junto com as tarefas de back do mesmo módulo, porque o contrato está na spec.

## Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Herança `JOINED` + `Specification` com filtro em campo da subclasse (placa) | Médio | T8 testa cedo; a busca por placa usa `treat()` ou subquery, decidida no T10 |
| Testcontainers exige Docker rodando durante `./mvnw test` | Médio | Docker já instalado; T1 valida |
| Prazo: 22 tarefas só na Parte 1 | Alto | Corte previsto: T20 vira só lista e formulário simples; T16 sem linha do tempo visual |
| ViaCEP fora do ar na avaliação | Baixo | Preenchimento manual sempre disponível |
| Spring Boot 4 usa Jackson 3 (`tools.jackson`); exemplos antigos usam Jackson 2 | Baixo | Não fixar `jackson-databind` 2.x (o Medflow fixa; não copiar) |

## Questões abertas

Nenhuma. Versão (4.1.1, igual ao Medflow) e dependências aprovadas em 22/10/2026.
