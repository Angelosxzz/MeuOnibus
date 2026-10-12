# Itinerários e referência da consulta

## Correção de 12/10/2026

O botão de ida/volta substituía o ponto do GPS/mapa pelas coordenadas da parada selecionada. Portanto, a distância exibida podia ser da parada até o desenho da via, embora o usuário entendesse que era medida da posição dele. Agora a consulta ativa é preservada e a tela identifica sua origem (GPS, mapa ou parada), coordenadas e precisão. GPS sem precisão conhecida ou com incerteza acima de 100 m não produz uma previsão aparentemente exata; o app espera uma leitura melhor ou permite escolher no mapa.

Outro risco confirmado por leitura: RoadGeometry gerava caminhos de carro entre as paradas e os consumia como se fossem itinerários de ônibus. A criação automática foi desativada. Somente geometria versão 3, com itineraryConfirmed=true, origem admin-traced/official e referência da confirmação, pode alimentar previsões por ponto. Dados antigos continuam no cache e as tabelas continuam disponíveis; não são promovidos automaticamente a percursos confirmados.

## Cadastrar no app de administrador

1. Entre e abra Banco de dados → Desenhar itinerários reais no mapa.
2. Escolha a linha, parada de saída e parada de chegada do trecho. As duas coordenadas precisam estar cadastradas e confirmadas.
3. Marque as ruas na ordem do percurso. Inclua todas as curvas; distância máxima de 250 m entre pontos. Pode importar um GeoJSON LineString ou Feature, com coordenadas na ordem longitude/latitude; o banco armazena latitude/longitude.
4. Use Desfazer ou Limpar para corrigir o rascunho. O GPS mostra sua posição em azul, sem adicionar pontos automaticamente.
5. Revise a linha, informe o tempo médio apenas deste trecho e como conferiu o caminho. Confirme explicitamente que o ônibus passa pela linha desenhada. O desenho deve começar e terminar a até 50 m das paradas, com no máximo 1000 pontos.
6. Publicar trajeto é a ação explícita autenticada que envia para todos os aparelhos. Revise cada sentido separadamente: sentido inverso não é inventado copiando a ida.

Não é preciso uma chave do Google Maps. Um caminho de navegação pode servir de rascunho, mas deve ser ajustado às ruas realmente usadas pelo ônibus. Marcar pontos pequenos e confirmar não prova automaticamente o itinerário: essa conferência depende do administrador e da fonte indicada.

Quando uma parada muda de coordenadas, os trechos ligados perdem a confirmação e precisam de revisão. Alterações apenas de nome preservam o percurso. Os passageiros podem visualizar a linha do trecho no mapa; o editor fica exclusivamente no admin. Sem itinerário confirmado perto da referência, a consulta explica a indisponibilidade e mantém a tabela manual. Havendo uma via confirmada mais distante, exibe a distância real até ela e permite conferir/escolher o ponto de embarque no mapa; não prevê passagem na posição atual distante da via.

## Evidência e fontes

A base GTFS oficial Arce disponível em 12/10/2026 (arquivo gtfs_ARCE_20260925_0.zip) foi baixada e examinada: 138 linhas, sem as três linhas municipais identificadas por esses nomes. Linhas intermunicipais que passam na mesma localidade não foram substituídas pelas municipais. Não foi encontrada uma geometria completa verificável dessas três linhas nas fontes consultadas.

A comparação das geometrias de Tabatinga já cadastradas com a malha CE-065 do OpenStreetMap, fora do centro, encontrou 97 pontos por sentido e nenhum desvio acima de 100 m. Essa verificação geométrica de corredor não prova as curvas do itinerário no centro nem reproduz a posição real do usuário. Uma prévia visual foi preparada fora do repositório. O administrador confirmou os outros três trechos e informou a correção de Pau Serrado → Shopping via CAPS Geral, Drogasil, Rua Doutor João Bezerra e Pague Menos. O trecho foi recalculado com pontos intermediários nas vias, seguindo Avenida Senador Almir Pinto → Rua Doutor Argeu Braga Herbster → Rua Mundica Paula → Rua Doutor João Bezerra → Rua Coronel Antônio Botelho. As três referências públicas foram localizadas no OSM; a farmácia Pague Menos considerada é a da Rua Coronel Antônio Botelho. Mantidos os 18 minutos da tabela, sem substituir pelo tempo de carro.

Fontes: [Arce, GTFS](https://www.arce.ce.gov.br/download/licitacoes-contratos-editais-formularios-e-gtfs/), [arquivo oficial de 25/09/2026](https://www.arce.ce.gov.br/wp-content/uploads/sites/53/2018/11/gtfs_ARCE_20260925_0.zip), [OSRM Route Service](https://project-osrm.org/docs/v5.24.0/api/#route-service), [OpenStreetMap API](https://wiki.openstreetmap.org/wiki/API_v0.6#Retrieving_map_data_by_bounding_box:_GET_/api/0.6/map), [atribuição e licença OSM](https://www.openstreetmap.org/copyright).

## Validação e limites

- RoadForecastCheck reproduziu o uso indevido de um caminho não confirmado a aproximadamente 13 m. Depois, esse caminho é ignorado; via confirmada a mais de 400 m não é associada como se estivesse a 13 m.
- ItineraryPolicyCheck verifica confirmação, origem, sentido, início/fim, continuidade e precisão do GPS. Três mutações (remover confirmação, ampliar precisão aceitável para 500 m e aceitar atalhos de 1000 m) foram detectadas.
- check-itineraries.mjs teve quatro falhas antes das correções; agora as seis verificações passam. Cobre validação e invalidação seletiva por proposta de alteração. Integra npm test e CI.
- Toda a suíte scripts/check.ps1 passou, e o Worker empacotado passou node --check.
- Os dois apps e a instrumentação final foram compilados em cópias QA, com lint sem erros (45 avisos admin, 42 passageiro). Avisos não foram ocultados.
- Prévia Leaflet/OSM aberta no navegador, mapa carregado e seleção dos sentidos verificada.
- Após reconectar o S21 FE, as seis verificações de instrumentação passaram (audit=PASS, failures=0), incluindo ida/volta preservando o ponto escolhido. A versão passageiro 0.6.0 foi instalada com sucesso sobre a anterior, com certificado igual, e mostrou as previsões do itinerário confirmado. GPS real e o editor de itinerários do admin ainda precisam de verificação manual.
- Os quatro percursos preparados passaram no validador da API e na mesma política Java usada no Android. O corpo de publicação tem 78.548 bytes, dentro do limite de 200.000; revisão esperada da nuvem 11. Umarizeiras e Amanari não receberam percursos inventados: as origens continuam sem confirmação e precisam de cadastro.
- Após aprovação explícita da prévia pelo administrador, os quatro trechos foram aplicados ao banco na revisão 12. O payload anterior completo, revisão 11 e timestamp foram preservados localmente e restaurados em SQLite em memória para verificação. O conector de inspeção do D1 truncou o payload: não se afirma exportação integral do banco. A API em produção e o APK no link público continuam anteriores: duas emissões de credencial de publicação retornaram validade já vencida. APKs 0.6.0 (passageiro) e 0.4.0 (admin) estão disponíveis localmente; o segundo ainda não foi instalado.

## Voltar ao código anterior

Branch anterior intacta: audit/2026-10-12-improvements, HEAD dc00aeb0bf53dbdbe4a2b22204e96b0144d1b7fa. Com árvore limpa, git switch audit/2026-10-12-improvements volta ao código anterior a esta correção. Um bundle completo desse estado foi criado e verificado fora do repositório. O backup integral anterior à auditoria continua disponível no ambiente local; não inclui uma cópia integral do D1 nem dados privados internos do celular.
