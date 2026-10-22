# Capability map: F.O.G.O.

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `auth` | Login JWT, perfis gestor e operador | — |
| `equipment` | Cadastro de equipamentos: tipo, características por tipo, localização, situação, manutenção, histórico | `auth` |
| `agent` | Cadastro de agentes: órgão, situação, base, posição, veículo atual, histórico | `auth`, `equipment` (veículo) |
| `operations` | Central: mapa, indicadores, filtros, alertas, telemetria simulada | `equipment`, `agent` |
| `forecast` | Previsão: risco por região e horário, anomalias, recomendações | `operations` (histórico) |

Ordem de construção: `auth` → `equipment` → `agent` → `operations` → `forecast`

Specs: `.specs/features/<módulo>/spec.md`. Parte 1 do desafio = `auth`, `equipment`, `agent`.
