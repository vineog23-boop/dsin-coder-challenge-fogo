# Plano: Parte 2 (central de operações)

Spec: `.specs/features/operations/spec.md` (aprovada em 2026-10-26). Tarefas: `.specs/features/operations/tasks.md`.
Numeração continua a da Parte 1 (T23 em diante). Execução em **dois grupos**, como na Parte 1: G4 (back) e G5 (front + entrega).

## Visão geral

Uma tela "Central" de leitura, montada sobre os dados da Parte 1: mapa, indicadores e alertas por três `GET` em `/api/operations`, mais uma telemetria simulada que move os agentes no perfil `demo`. Sem tabela, migration ou dependência nova.

## Decisões de arquitetura

- **`operations` só lê.** O pacote `operations/{web,application}` não tem `domain` nem `persistence` próprios. As consultas de agregação ficam nos repositories de `equipment` e `agent` (métodos `@Query` com projeções), e o `operations` depende deles, como o `agent` já depende de `equipment`.
- **Agregação no banco:** contagens por situação e por região e o "desde" da falha (`max(changed_at)`) são feitos em SQL/JPQL, nunca carregando as entidades. Com ~190 pontos não há problema de desempenho, mas o mesmo código serve para milhares.
- **Mapa com projeção leve:** a consulta devolve só as colunas do marcador, sem `JOIN` das tabelas filhas. Assim a herança `JOINED` não pesa.
- **Telemetria liga por propriedade:** `@ConditionalOnProperty(app.telemetry.enabled=true)` + `@EnableScheduling`. O compose liga (`APP_TELEMETRY_ENABLED=true`, junto do perfil `demo`); os testes com perfil `demo` não ligam, então o agendamento não interfere nos testes de seed.
- **"Hoje" e "desde" pelo `Clock` injetado**, no fuso `America/Sao_Paulo`, como a Parte 1.
- **Front:**
  - polling num hook (`usePolling`) que pausa com `document.hidden`;
  - ícones como `L.divIcon` com SVG ou emoji, para não depender de imagens nem do problema de assets do Leaflet com o Vite;
  - filtros na URL, igual às listas.

## Grafo de dependências

```
T23 consultas + /summary + /map ──┬── T24 /alerts
                                  └── T25 telemetria
                ─── Checkpoint F (API) ───
T26 página Central: mapa + filtros + polling (usa /map)
   └── T27 indicadores + alertas na Central (usa /summary, /alerts)
          └── T28 tela inicial, README e verificação do zero
                ─── Checkpoint G (aceite da spec) ───
```

T24 e T25 são independentes entre si, ambas dependem só de T23. T26 pode começar assim que o contrato do `/map` existir.

## Tarefas (resumo)

| # | Tarefa | Grupo | Tamanho |
|---|---|---|---|
| T23 | Consultas de agregação + `GET /summary` e `GET /map` | G4 | M |
| T24 | Alertas calculados + `GET /alerts` | G4 | M |
| T25 | Telemetria simulada (perfil `demo`) | G4 | S |
| ✔ | **Checkpoint F**: API da central completa e testada | | |
| T26 | Front: página Central com mapa, ícones, popups, filtros e polling | G5 | M |
| T27 | Front: indicadores, tabela por região e alertas na Central | G5 | M |
| T28 | Central como tela inicial, README (Central + roteiro do vídeo) e verificação do zero | G5 | S |
| ✔ | **Checkpoint G**: critérios de aceite da spec `operations` marcados | | |

## Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Consulta com herança `JOINED` + `@Embedded` (região) + `max(changed_at)` agrupado | Médio | T23 começa pela integração (Testcontainers) com dados conhecidos; JPQL com projeção em record; SQL nativo se o JPQL complicar |
| `@Scheduled` rodando durante os testes de seed (mesmo perfil `demo`) | Médio | `@ConditionalOnProperty` desligado por padrão; só o compose liga |
| Ícones do Leaflet quebrados no build (assets de imagem) | Baixo | `L.divIcon` com SVG ou emoji, sem imagem |
| Leaflet não roda no jsdom | Baixo | Mapa mockado nos testes de página, como na Parte 1; `mapIcons` testado como função pura |
| Polling sobrecarregando com várias abas | Baixo | Pausa com a aba oculta; 3 GETs leves a cada 10 s |
| Prazo (27/10 23:59) | Alto | Corte previsto: T27 sem a tabela por região (só cartões) e alertas sem paginação no front (primeira página) |

## Questões abertas

Nenhuma. Decisões da clarificação e da aprovação da spec registradas em `.specs/STATE.md`.
