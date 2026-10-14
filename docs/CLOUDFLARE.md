# Hospedagem da versão web

Projeto Pages: **meu-onibus**. URL: https://meu-onibus.pages.dev/web/.

O site está na conta Cloudflare do proprietário. Os dados continuam na API original: não houve migração do banco, nem mudança do endereço usado pelo Android ou pelo administrador.

## Preparar uma publicação

Com Node.js 24, dentro de `meu-onibus`:

```sh
npm test
npm run build:cloudflare
```

Envie o conteúdo de `dist/cloudflare` pelo painel Workers & Pages → meu-onibus → Create deployment. Pode selecionar a pasta inteira ou um ZIP contendo `_worker.js` e a pasta `web` na raiz. Aguarde todos os arquivos e só então clique em Deploy site. Não recarregue durante o envio. Nenhuma configuração local, chave ou aplicativo de administração entra nesse pacote.

O `_worker.js` encaminha apenas leitura da tabela, leitura do estado de sugestões e envio público de propostas para a API existente. Ele bloqueia operações administrativas e não encaminha cookies ou autorização. O banco mantém validação, revisão e aprovação das propostas.

Pages converte `/web/index.html` em `/web/`; o gateway não deve redirecionar `/web/` de volta para `index.html`, pois isso cria um ciclo.

## Verificar a publicação

- Abra `/web/` e confira mapa, tabela e instalação na tela inicial.
- Confira `/api/schedule`: deve retornar JSON válido e a revisão da tabela.
- Confira `/api/suggestions/settings`: deve refletir o estado configurado pelo administrador.
- Operações administrativas não devem estar disponíveis pelo gateway.
- Teste GPS e instalação no Safari/iPhone e no Android, sem confundir estimativa com rastreamento ao vivo.

## Pendência comprovada

A página publicada abre, mas a consulta `/api/schedule` pelo gateway Cloudflare retornou **503**, enquanto a API original respondeu **200**. A web mostra a tabela salva e avisa sobre sincronização. A causa da conexão entre hospedagens ainda precisa de diagnóstico; não considerar sincronização ou envio de sugestões em produção como verificados até essa correção. Não migrar o banco para contornar essa falha sem backup e plano de compatibilidade.

## Reverter

Na aba Deployments, escolha uma versão anterior válida e use o fluxo de rollback do painel. Preserve os ZIPs utilizados e registre os resultados antes de publicar. O backup anterior à implementação web tem um bundle Git e instruções locais em `Backups/Web-iPhone-2026-10-13`, fora do repositório.

Referências: [Direct Upload](https://developers.cloudflare.com/pages/get-started/direct-upload/), [Advanced mode](https://developers.cloudflare.com/pages/functions/advanced-mode/).
