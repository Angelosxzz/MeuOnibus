# API

Base: servidor HTTPS configurado em CloudDatabase. Respostas de dados em JSON. Para operações administrativas, envie `Authorization: Bearer <ADMIN_KEY>`; a chave deve permanecer em segredo no ambiente do servidor e ser informada apenas no app administrador.

| Método | Caminho | Uso |
| --- | --- | --- |
| GET | /api/schedule | Consulta pública da tabela e revisão |
| PUT | /api/schedule | Atualização autenticada dos dados |
| GET | /api/suggestions/settings | Estado público do recebimento |
| PUT | /api/suggestions/settings | Alterar accepting, autenticado |
| POST | /api/suggestions | Proposta de nova parada ou alteração |
| GET | /api/suggestions | Fila administrativa, autenticado |
| POST | /api/suggestions/:id/review | Aprovar/rejeitar, autenticado |
| GET | /api/app-release | Pacote, versão, URL, tamanho e SHA-256 do APK |
| GET / HEAD | /downloads/Meu-Onibus.apk | APK dos passageiros |
| GET | /map-picker | Seletor de mapa OpenStreetMap |

Exemplo de proposta: `{"operation":"update","target":"stop:Rodoviária","name":"Rodoviária","lat":null,"lon":null,"reason":"Ajustar o nome"}`. Para adicionar: operation add, target vazio, name, route e coordenadas válidas. Coordenadas de sugestões são dados enviados conscientemente pelo autor; não publique a fila.

Revisão: `{"decision":"approve"}` ou `{"decision":"reject"}`. Recebimento: `{"accepting":false}`.

Erros relevantes: 400 dados inválidos; 401 chave ausente/incorreta; 403 recebimento pausado; 409 proposta já revisada ou conflito; 429 limite de envio. Os contratos detalhados e casos de conflito são exercitados por check-suggestions.mjs. As tabelas usam schemaVersion 2; preserve a revisão recebida ao editar e consulte worker/index.js para as validações completas.
