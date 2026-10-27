# F.O.G.O.

Inventário de equipamentos IoT de trânsito e segurança e dos agentes que atuam em campo em Presidente Prudente/SP, com uma central de operações ao vivo. São as Partes 1 (inventário) e 2 (central) do desafio F.O.G.O. (DSIN Coder Challenge, Protocolo PROMETHEUS).

Cada equipamento e cada agente guarda o **histórico** de cada mudança de situação e manutenção. Esse histórico alimenta o mapa da central (Parte 2) e as previsões (Parte 3).

## Como rodar

Pré-requisito: Docker com Docker Compose.

```bash
docker compose up --build
```

Abra **http://localhost:3000**: depois do login, a tela inicial é a **Central**. Na primeira subida, o banco vem com dados de demonstração: 150 equipamentos e 40 agentes, com cerca de 6 meses de histórico.

| Usuário | Senha | Perfil | Pode |
|---|---|---|---|
| `gestor` | `gestor123` | Gestor | tudo: cadastrar, editar, mudar situação, registrar manutenção |
| `operador` | `operador123` | Operador | consultar e atualizar a posição de agentes |

- **Swagger:** http://localhost:3000/swagger-ui/index.html. Faça `POST /api/auth/login`, copie o `accessToken` e clique em **Authorize**.
- **Health:** http://localhost:3000/api/actuator/health
- **Recomeçar do zero:** `docker compose down -v && docker compose up --build` apaga o banco e recria os mesmos dados de demonstração.
- **Sem `.env`:** o compose já traz valores de desenvolvimento. Para mudar senhas ou o segredo do JWT, copie `.env.example` para `.env`.
- **Portas:** 3000 (front e API) e 5433 (PostgreSQL, para inspeção).

### Desenvolvimento (opcional)

```bash
docker compose up -d db                             # só o banco
cd backend && ./mvnw spring-boot:run                # API em :8080 (sem perfil demo, banco vazio)
cd frontend && npm install && npm run dev           # front em :5173, com proxy de /api para :8080
```

Testes: `cd backend && ./mvnw test` (precisa de Docker, porque usa PostgreSQL real via Testcontainers) e `cd frontend && npm test`.

## O que existe

**Central de operações** (`/central`, Parte 2)
- **Mapa da cidade:** os equipamentos têm um ícone por tipo e a borda na cor da situação; os agentes em atividade aparecem como círculo na cor da situação, na posição em campo ou, sem ela, na base. Clicar num marcador abre o resumo, com link para o detalhe.
- **Indicadores:** equipamentos com falha, manutenção imediata, manutenção atrasada e agentes em atividade, mais uma tabela por região (falhas × agentes).
- **Alertas calculados na hora:** equipamento com falha ou roubado, região sem agente em atividade e manutenção atrasada, ordenados por severidade e antiguidade.
- **Filtros:** camadas, tipo, situação, região e órgão. Ficam na URL.
- **Atualização:** a cada 10 s, com pausa quando a aba fica oculta. Mudar um equipamento para "com falha" aparece na central em até 10 s.
- **Telemetria simulada:** no compose, os agentes em serviço andam perto da base a cada 10 s (`APP_TELEMETRY_ENABLED`). Isso não grava histórico.

**Equipamentos**
- 14 tipos (radar, câmera, balança, táxi, viaturas, patinete, cancela...). Cada grupo tem características próprias: o radar tem modalidade e limite de velocidade; o veículo tem placa e passageiros.
- Lista com filtros (tipo, situação, região, órgão, manutenção) e busca por série, marca, modelo ou placa. Os filtros e a página ficam na URL.
- Formulário cujos campos mudam conforme o tipo. O CEP preenche o endereço pelo ViaCEP (se o serviço falhar, dá para digitar), e a coordenada é marcada clicando num mini-mapa. Os erros da API aparecem no campo certo.
- Detalhe com características, mapa e linha do tempo de situações e manutenções.
- Mudar situação: ir para "com falha" exige manutenção imediata.
- Registrar manutenção: o equipamento volta a operar e a próxima manutenção é agendada pelo intervalo.

**Agentes**
- Agente de trânsito, guarda municipal e policial militar. O órgão é derivado do tipo.
- Viatura opcional. O seletor só oferece viaturas do mesmo órgão que não estejam desativadas nem roubadas, e a API confere de novo.
- Mudar para "fora de serviço" ou "afastado" grava histórico e desvincula a viatura.
- O operador pode atualizar a posição em campo, mas não a situação.

**Regras gerais**
- Nada é excluído. Equipamento sai de uso como "desativado"; agente, como "afastado".
- Erros em Problem Details (RFC 9457), por exemplo:

  ```json
  { "type": "urn:fogo:problem:validation", "status": 400, "errors": [{ "field": "radar.speedLimitKmh", "message": "..." }] }
  ```

  Os códigos usados: `400` dado inválido, `401` sem login, `403` perfil sem permissão, `404` não encontrado, `409` duplicado (série, placa, matrícula), `422` regra de negócio.

## Dados de demonstração

Gerados com semente fixa (`Random(42)`): duas subidas do zero geram os mesmos dados, com datas contadas a partir do dia da subida. As coordenadas caem na área urbana de Presidente Prudente, e a região sai do quadrante em relação ao centro.

O histórico tem um **padrão plantado de propósito** para a Parte 3 encontrar. Está documentado em `DemoEquipmentSeeder` e `DemoAgentSeeder`:
- **Região:** mais falhas no Norte (×2,5) e no Leste (×1,8).
- **Tipo:** radar e câmera falham 30% mais, porque ficam expostos na rua.
- **Horário:** 60% das falhas entre 18h e 23h.
- **Fila de reparo:** um quarto das falhas espera de 3 a 15 dias, por isso parte do parque está parada hoje.
- **Agentes:** os de base no Norte e no Leste atendem ocorrências com mais frequência.

## Decisões técnicas

| Decisão | Por quê |
|---|---|
| Java 21, Spring Boot 4.1.1, PostgreSQL 17, Flyway | Stack pedida pelo desafio e conhecida pelo autor; schema versionado e Hibernate só em `validate` |
| Back-end por feature (`equipment`, `agent`, `auth`, `shared`), cada uma em `web → application → domain` + `persistence` | Cada assunto fica junto, e as regras de negócio não dependem de HTTP |
| Características por tipo com **herança JPA `JOINED`** (`equipment` + `radar`, `camera`, `truck_scale`, `vehicle`, `scooter`) | Colunas explícitas com `NOT NULL`/`CHECK` reais; um tipo novo com características exige migration (trade-off aceito, em vez de JSONB) |
| Regras na entidade (`changeStatus`, `registerMaintenance`) e no service; `CHECK` no banco como rede de segurança | A regra fica num lugar só; nem o seed nem um bug gravam radar de velocidade sem limite ou agente fora de serviço com viatura |
| JWT stateless com perfis `MANAGER`/`OPERATOR` | Sem sessão no servidor; o operador só pode atualizar a posição do agente |
| Histórico desde a Parte 1 (`*_status_history`, `equipment_maintenance`) | É o insumo das previsões da Parte 3 |
| Central sem tabela própria: contagens em JPQL com projeções em `record`, alertas calculados a cada consulta | Nada a sincronizar: a central sempre mostra o estado atual. O mapa não é paginado (exceção registrada na spec), mas tem teto de 2.000 pontos por camada |
| Atualização por polling (10 s), não WebSocket | Simples atrás do nginx e fácil de testar; suficiente para o volume da cidade |
| Front React + TypeScript + Vite + Leaflet, servido pelo nginx com `/api` no mesmo endereço | Sem CORS; um comando sobe tudo |
| Testes: unitário de service com mocks, integração com PostgreSQL real (Testcontainers, nunca H2), MockMvc com a segurança real, Vitest + Testing Library no front | Os testes cobrem o banco de verdade, inclusive as `CHECK` e a herança |

Specs, plano e decisões ficam em `.specs/`.

## Uso de IA

O desafio pede que se declare quais IAs foram usadas e como.

- **Claude Code (Anthropic, modelo Claude Opus):** usado do início ao fim no fluxo spec → plano → implementação por tarefas → revisão:
  - **Specs e plano:** `.specs/features/*/spec.md`, `tasks.md` e `plan-parte-1.md` foram escritos com a IA e aprovados pelo autor antes de qualquer código.
  - **Implementação:** feita pela IA tarefa a tarefa, seguindo as regras do projeto (`AGENTS.md`). Regras de negócio foram escritas em TDD: o teste primeiro, visto falhando, e depois o código.
  - **Subagents do projeto (`.claude/agents/`):** `testador` escreveu os testes de controller, integração e front; `revisor` e `arquiteto` ficam disponíveis para revisão do diff e decisões de design.
  - **Verificação:** a IA rodou testes, build e lint e conferiu as telas num navegador automatizado (Playwright), em largura de celular e de desktop, antes de cada commit. Cada commit foi aprovado pelo autor.
- **O papel do autor:** escopo, decisões de arquitetura (herança `JOINED`, JWT, histórico desde a Parte 1, sem exclusão física), aprovação de specs, planos e commits.

> **Autor, confirme antes de entregar:** se usou outras IAs (ChatGPT, Gemini, Copilot etc.), acrescente aqui quais e para quê.

## Roteiro do vídeo (≈ 4 min)

1. `docker compose up --build` e abrir http://localhost:3000.
2. Entrar como **gestor**: a **Central** abre com o mapa, os indicadores e os alertas. Mostrar o popup de um marcador, filtrar por "Com falha", desligar a camada de agentes e apontar um agente andando (telemetria).
3. Em outra aba, mudar um equipamento para "Com falha" e voltar à central: em até 10 s o marcador fica vermelho, o indicador sobe e surge o alerta.
4. Lista de equipamentos: filtrar Radar + Norte + "Com falha" e mostrar que a URL guarda a busca.
5. **Novo equipamento:**
   - trocar o tipo (Radar → Táxi) e mostrar os campos mudando;
   - salvar um radar de velocidade sem limite e mostrar o erro no campo;
   - preencher o CEP (ViaCEP) e clicar no mapa.
6. **Detalhe:** mudar para "Com falha" (manutenção vira imediata) e registrar a manutenção (volta a operar, próxima data agendada). O histórico aparece na hora.
7. **Agentes:** criar um policial militar e mostrar que o seletor só oferece viaturas da PM. Mudar para "Fora de serviço" e mostrar que ele perde a viatura.
8. Entrar como **operador** e mostrar que as ações de escrita somem.
9. **Swagger:** login, Authorize e um `PATCH /api/agents/{id}/position`.
10. Fechar com o padrão do seed (Norte/Leste, noite) como gancho para a Parte 3 (previsão).
