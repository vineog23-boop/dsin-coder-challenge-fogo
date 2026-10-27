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
- **Indicadores:** total de equipamentos, operacionais, indisponíveis (com falha, em manutenção ou roubados), percentual de disponibilidade (operacionais sobre os em uso, sem desativados e não instalados), agentes em atividade, equipamentos com falha, manutenção imediata, manutenção atrasada e manutenção nos próximos 7 dias. Mais uma tabela por região (falhas × agentes).
- **Alertas calculados na hora:** equipamento com falha ou roubado e região sem agente em atividade (alta), manutenção atrasada (média) e manutenção prevista para os próximos 7 dias (baixa), ordenados por severidade e antiguidade.
- **Filtros:** camadas, tipo, situação, região, órgão e bairro (lista com os bairros existentes). Ficam na URL.
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

O desafio pede que se declare quais IAs foram usadas e que o autor mostre entender o que foi construído. Esta seção conta como o projeto foi conduzido.

**IA usada:** Claude Code (Anthropic, modelo Claude Opus), em todas as etapas.

### Por que spec-driven

Com prazo curto e uma IA escrevendo boa parte do código, o maior risco é ela decidir sozinha: inventar regra, mudar contrato de API, fazer mais do que o pedido. Por isso o projeto seguiu um fluxo **spec-driven**: nenhuma linha de código antes de uma spec aprovada pelo autor. A spec é o contrato entre o autor e a IA, e é contra ela que cada entrega foi verificada.

### Como o projeto foi conduzido

1. **Regras antes do código.** O `AGENTS.md` define stack, arquitetura por feature, estilo de código, padrão de testes e limites. Os limites dizem o que a IA faz sozinha, o que precisa perguntar antes (dependência nova, contrato de API, schema) e o que nunca faz (apagar ou enfraquecer teste que falha, commitar segredo). Há regras por camada em `.claude/rules/` e três subagents em `.claude/agents/`: `arquiteto`, `revisor` e `testador`.
2. **Decisões do autor, registradas.** Escopo, stack e modelagem saíram de uma entrevista e ficaram em `.specs/STATE.md`, cada uma com o motivo e o custo aceito. Exemplos: herança JPA `JOINED` em vez de JSONB, porque o autor consegue explicar e defender cada coluna; JWT com dois perfis; histórico gravado desde a Parte 1, já pensando na previsão.
3. **Uma spec por feature.** `.specs/features/{auth,equipment,agent,operations}/spec.md` traz modelo, regras de negócio numeradas, rotas da API, critérios de aceite e o que fica fora do escopo. Cada spec foi aprovada antes do plano.
4. **Plano em tarefas.** `.specs/plan-parte-1.md` (T1 a T22) e `.specs/plan-parte-2.md` (T23 a T28), com sete checkpoints (A a G). Cada checkpoint tem um critério verificável, por exemplo "login funciona no navegador com os dois perfis".
5. **Implementação tarefa a tarefa.** A IA implementa a menor mudança que resolve a tarefa. Regras de negócio seguem TDD: o teste é escrito primeiro, visto falhando, e só então vem o código. Antes de qualquer tarefa ser dada como pronta, a IA roda testes, lint e build e confere as telas num navegador automatizado (Playwright), em largura de celular e de desktop. Com o prazo apertado, a partir de 26/10 as tarefas passaram a ser feitas em grupos até o próximo checkpoint, mantendo um commit por tarefa.
6. **Aprovação do autor em cada passo.** Cada commit foi mostrado e aprovado. Mudança de contrato, de schema ou dependência nova só entrava depois de uma pergunta explícita. Exemplo: em 27/10, ao comparar o sistema com o enunciado, a IA apontou três lacunas na Parte 2 (indicadores de disponibilidade, filtro por bairro e destaque de manutenção próxima). O autor aprovou a mudança de contrato, e as lacunas foram fechadas com testes escritos antes do código.
7. **Memória entre sessões.** O `.specs/STATE.md` guarda decisões, o ponto em que o trabalho parou e os cuidados descobertos no caminho, para cada sessão recomeçar de onde a anterior terminou, sem depender da memória da conversa.

### Quem fez o quê

| Autor | IA |
|---|---|
| Escopo e prioridades | Redigir specs e planos a partir das decisões do autor |
| Decisões de arquitetura e seus custos | Implementar tarefa a tarefa, dentro das regras do `AGENTS.md` |
| Aprovar specs, planos, commits e qualquer mudança de contrato | Escrever e rodar os testes, o lint, o build e a conferência no navegador |
| Conferir o resultado contra o enunciado | Apontar riscos, lacunas e trade-offs antes de decidir |

### Como conferir

- As specs, os planos e as decisões estão em `.specs/`; as regras dadas à IA, em `AGENTS.md` e `.claude/`.
- O `git log` mostra a evolução em commits pequenos, um assunto por commit, na ordem do plano.
- Os 243 testes (183 no back-end e 60 no front-end) são a evidência de que cada regra da spec foi implementada.
- O material de apresentação (slides e roteiro do vídeo) também foi produzido com o Claude Code, usando a ferramenta Mira, fora deste repositório.

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
