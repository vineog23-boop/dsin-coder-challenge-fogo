# fogo

## Comandos

- Subir tudo (banco + API + front): `docker compose up --build`
- Só o banco para desenvolvimento: `docker compose up -d db`
- Back-end (pasta `backend/`): testes `./mvnw test` · um teste `./mvnw test -Dtest=NomeDoTeste` · build `./mvnw -q package` · rodar `./mvnw spring-boot:run`
- Front-end (pasta `frontend/`): `npm run dev` · `npm run lint` · `npm run build` · `npm test`

## Stack

Java 21 · Spring Boot 4.1.1 · Maven · PostgreSQL · Spring Data JPA/Hibernate · Flyway · JUnit 5 + AssertJ + Mockito · Testcontainers
React + TypeScript + Vite · Leaflet (mapa) · Node 24

## Arquitetura

- Monorepo: `backend/` (API REST), `frontend/` (dashboard com mapa), `docker-compose.yml` na raiz.
- Pacote base do back-end: `br.com.dsin.challengefogo`.
- Back-end por feature: `<feature>/{web,application,domain,persistence}`; o transversal fica em `shared/`. Features previstas: `equipment`, `agent`, `operations` (central), `forecast` (previsão).
- Dependências: `web → application → domain`; `persistence` implementa o acesso a dados. `domain` não conhece Spring Web.
- Previsão (parte 3) começa com regras e estatística sobre o histórico. Biblioteca de ML só com aprovação.
- Decisões e handoff entre sessões: `.specs/STATE.md`. Specs: `.specs/features/<nome>/`.

## Estilo de código

- Injeção por construtor, campos `private final`. Sem Lombok.
- DTOs são `record`, nomeados `EquipmentDtoRequest` / `EquipmentDtoResponse`; o de resposta tem construtor que recebe a entidade.
- Entidades JPA têm sufixo `Entity` (`EquipmentEntity`) e nunca saem na API.
- Mudança de estado por método da entidade (`equipment.update(...)`), sem setters soltos.
- Busca sem resultado: `orElseThrow` com exceção específica (`EquipmentNotFoundException(id)`).
- Erros da API em Problem Details (RFC 9457) no `GlobalExceptionHandler`; tipos em `ProblemTypes` (`urn:fogo:problem:<erro>`).
- Bean Validation no DTO de entrada, mensagens em português.
- IDs `UUID`; datas em `Instant`. Listagens com `Page`/`Pageable`, nunca sem limite.
- Schema só por Flyway; Hibernate em `ddl-auto=validate`.
- Javadoc em português explicando o porquê, não o óbvio.

Exemplo de estilo (ilustrativo, adaptado do estilo global; troque por código real quando existir):

```java
public EquipmentDtoResponse findById(UUID id) {
    EquipmentEntity equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new EquipmentNotFoundException(id));

    return new EquipmentDtoResponse(equipment);
}
```

## Testes

- Service: unitário com `mock(Repository.class)`, sem subir Spring.
- Persistência: PostgreSQL real via Testcontainers, nunca H2.
- Nome do método em português: `deveXQuandoY` / `naoDeveXQuandoY`. Arrange / act / assert separados por linha em branco.

## Git

- Commits curtos no imperativo, em português; um assunto por commit.

## Limites

**Sempre**
- Rodar os testes afetados e mostrar a saída antes de dizer que terminou.
- Manter o projeto subindo com `docker compose up --build` (o enunciado avalia "rodar de primeira").
- Em mudança de front, verificar no navegador em largura de celular e de desktop.

**Perguntar antes**
- Adicionar dependência (back ou front).
- Mudar contrato da API (endpoint, payload, status).
- Mudar schema.

**Nunca**
- Commitar segredos ou `.env`.
- Apagar ou enfraquecer teste que falha.
