# Meu Ônibus na web

Versão para passageiros em `meu-onibus/web`, instalável na tela inicial do iPhone e Android. Usa a mesma API de horários e sugestões do app Android. Não contém funções nem credenciais de administrador.

## Executar e verificar

Com Node.js 24, entre em `meu-onibus` e execute `npm test`. O build copia os arquivos para `dist/web` e compartilha o validador dos itinerários com a API. Sirva essa pasta junto do Worker e de um banco D1 de teste; um servidor de arquivos sozinho não fornece os endpoints `/api`.

Entrada publicada prevista: `/web/index.html`; `/web` redireciona para ela. A raiz e os downloads Android mantêm o comportamento existente. A hospedagem precisa de HTTPS para GPS e service worker.

## Comportamento

- Consulta por parada, linha e destino, horários centrais e variação de Tabatinga.
- Mapa OpenStreetMap com somente os itinerários confirmados. O ponto manual e o GPS servem de referência; vias a mais de 200 m não recebem previsão como se estivessem ao lado.
- GPS exige precisão de até 100 m. Localização de consulta não é salva nem enviada à API.
- Sugestões são enviadas explicitamente para análise; a publicação oficial depende do administrador. O estado de recebimento é conferido no servidor.
- Horários ficam disponíveis na última tabela salva quando a sincronização falha. Mapa e envio de sugestões precisam de internet. O service worker não guarda APIs, mapa externo ou operações de escrita.
- No Safari, abra Compartilhar → Adicionar à Tela de Início. Isso instala a versão web; não é um aplicativo nativo da App Store.

Não há rastreamento ao vivo dos veículos. Umarizeiras não tem trajeto confirmado para projeção e Amanari aguarda horários confirmados. A tabela diária não possui calendário de feriados.

## Verificação local — 14/10/2026

API: testes de sugestões com SQLite, versões Android, proteção de entradas e itinerários passaram. Web: validação de tabela, fuso do Ceará, limites de precisão, margem de parada, projeção e rejeição de rua fora do itinerário passaram. Uma alteração intencional da margem da Rodoviária fez o teste falhar e foi revertida.

Navegador Chromium: seleção no mapa retornou os dois sentidos; proposta local ficou pendente sem alterar a revisão 12; recebimento pausado bloqueou o formulário; erro de sincronização preservou tabela e ponto escolhido. Não foi verificado em Safari ou iPhone físico; GPS físico e modo avião ainda precisam de teste nesses aparelhos.

## Hospedagem

Cloudflare Workers com Static Assets e D1 é compatível com o backend atual. Migrar para uma conta própria exige autenticação nessa conta e decisão sobre o banco; não altere o endpoint dos apps Android sem planejar a compatibilidade. A publicação existente via Sites pode receber os arquivos web sem migração de banco.

Referências: [Cloudflare Static Assets](https://developers.cloudflare.com/workers/static-assets/), [cobrança e limites](https://developers.cloudflare.com/workers/static-assets/billing-and-limitations/), [instalação no iPhone](https://support.apple.com/en-ie/guide/iphone/iphea86e5236/ios), [política de mapas OSM](https://operations.osmfoundation.org/policies/tiles/).
