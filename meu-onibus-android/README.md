# Meu Ônibus — Android 0.5.0

## Atualizações do aplicativo

A partir de 0.5.0, o app consulta `/api/app-release` ao entrar e avisa quando houver uma versão mais nova. **Baixar e atualizar** obtém o APK por HTTPS, verifica tamanho, SHA-256, pacote, versão e assinatura e solicita a instalação ao Android. O usuário autoriza a instalação. **Agora não** permite continuar usando o app; **Verificar atualização do app** faz uma consulta manual.

Versões anteriores precisam instalar 0.5.0 manualmente uma vez para receber os avisos futuros. Publique a informação de versão e o APK juntos, mantendo a mesma chave de assinatura. O instalador recebe acesso temporário de leitura somente ao APK verificado via ApkProvider. O aviso distribui apenas o app dos passageiros.

Verificado no S21 FE com instalação de teste código 5 e APK novo código 6: aviso, download HTTPS, hash, tamanho, pacote e assinatura; rejeição de pacote diferente e assinatura diferente; provider somente leitura; configuração do Android solicitada sem concessão automática de permissão. Os testes de lógica também cobrem versões iguais ou anteriores, integridade do arquivo e endereço confiável.

App nativo em Java, Android 8 ou superior, para passageiros. APK: Meu-Onibus.apk. O app Admin é separado; nenhuma chave de administrador faz parte deste app.

## Previsão no ponto da pessoa

GPS ou escolha no mapa → projeção sobre as ruas → cartões separados por sentido. A associação é limitada a 200 m da pista. Referências públicas do OpenStreetMap e paradas cadastradas aparecem quando próximas (até 150 m). Elas não mudam o local da previsão e não são cadastradas como novas paradas.

Os tempos relativos dos segmentos de rua vêm do OSRM. O progresso é ponderado por esses tempos. Em Tabatinga, a duração entre paradas vem dos horários do mesmo carro nas duas paradas. Ônibus com saída anterior à hora atual continuam considerados enquanto sua passagem prevista for futura. Não se extrapola viagem sem o próximo horário do carro ou serviço para o dia seguinte. Os carros simulados não representam uma frota rastreada.

Umarizeiras usa as médias informadas: Rodoviária → Shopping 2 min; espera de 5 min em cada ponto; Shopping → Umarizeiras 40 min. Exemplo: Rodoviária 06:28–06:33; Shopping 06:35–06:40; Umarizeiras ~07:20. A origem de Umarizeiras ainda precisa de coordenadas confirmadas para os trechos até o bairro. Amanari não tem horários cadastrados.

## Dados e privacidade

Horários, trajetos e referências ficam no banco na nuvem e no cache SQLite do aparelho. roadSegments e landmarks são campos adicionais compatíveis com schemaVersion 2. Em mudanças de coordenadas, os trajetos antigos são descartados e os trechos afetados são recalculados com internet. O serviço de roteamento recebe somente as coordenadas públicas das paradas, nunca o GPS do passageiro. A escolha do ponto de consulta não publica dados ou sugestões.

Mapas: Leaflet/OpenStreetMap. O centro escolhido é aplicado no WebView local, sem enviar as coordenadas do passageiro na URL do servidor do app. O GPS fica na memória. O mapa carrega peças cartográficas de terceiros. Sem internet, utiliza os trajetos salvos. Geometrias correntes não são requisitadas novamente a cada consulta do GPS.

As ruas calculadas são trajetos rápidos de carro entre as paradas; não confirmam todo o itinerário do ônibus. Sem trânsito atual, atrasos ou rastreamento da frota. © OpenStreetMap contributors, ODbL.

## Compilar e verificar

Android Studio/Gradle, SDK 36 e Java 17. Alternativa: build-apk.ps1 com os argumentos Jdk, Platform, BuildTools e Keystore.

Cálculos: verification/run-checks.ps1 -Jdk <pasta-jdk>. Os casos cobrem projeção na pista, tempos intermediários, dois sentidos, ônibus já saídos, limites da tabela, referências próximas, dados indisponíveis e virada do dia. Mutações que removem a interpolação ou ignoram os tempos intermediários foram detectadas.

Validado no S21 FE: dois sentidos em um ponto intermediário, saídas passadas e futuras, mapa centralizado corretamente, seleção retornando à previsão, consulta manual e GPS real no mapa e no painel. Casos de horário usam hora de teste, sem afirmar que exista um ônibus fisicamente naquela posição.

Consulte Como-testar-Meu-Onibus.md, junto aos arquivos, para instalação e uso.

