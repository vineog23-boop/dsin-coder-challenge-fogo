---
paths:
  - "frontend/**"
---

# Regras do front-end

- TypeScript sem `any`; tipos das respostas da API declarados.
- Chamadas ao back-end isoladas em `src/lib/api/`, nunca `fetch` espalhado nos componentes.
- Toda tela que busca dados trata carregamento, erro e vazio.
- Componentes pequenos, um por arquivo, PascalCase. Elementos semânticos, `alt` em imagens, foco visível, rótulo em campos.
- Mapa (Leaflet): ícone por tipo de equipamento e cor por situação; detalhes ao clicar no marcador.
- Antes de declarar pronto: `npm run lint`, `npm run build` e conferir no navegador (celular e desktop).
