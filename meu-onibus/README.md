# API Meu Ônibus

Worker com D1/SQLite, tabelas públicas, autenticação administrativa, moderação de sugestões e distribuição do APK.

Node 24+: npm test executa os testes em memória; npm run build prepara dist/server/index.js; npm run build:release incorpora o APK dos passageiros.

Consulte [API](../docs/API.md), [desenvolvimento](../docs/DEVELOPMENT.md) e [publicação](../docs/RELEASING.md). Defina DB e ADMIN_KEY no provedor, sem versionar seus valores. A migração histórica de Google Maps permanece inativa; a interface atual usa OpenStreetMap.
