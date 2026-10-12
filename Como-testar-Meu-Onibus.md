# Meu Ônibus 0.5.0 — teste para passageiros

## Como receber novas versões

Instale 0.5.0 uma vez pelo [link público do APK](https://meu-onibus.miguel-angelo09.chatgpt.site/downloads/Meu-Onibus.apk). Depois, quando houver uma atualização, abrir o app com internet mostrará **Nova versão do Meu Ônibus**.

Toque em **Baixar e atualizar**. Caso o Android peça autorização, use **Abrir configuração**, autorize instalações pelo Meu Ônibus e volte ao app. Confirme a atualização no instalador do Android. **Agora não** deixa para depois. Quem estiver com a versão atual não recebe aviso.

Instale Meu-Onibus.apk em Android 8 ou superior. Em um celular que já tenha o app, instale por cima para atualizar. O ícone dos passageiros é verde.

1. Abra conectado à internet para atualizar horários, trajetos e referências locais.
2. Toque em **Usar meu GPS** e permita localização precisa. O aplicativo usa o ponto mais próximo dos trajetos estimados pela pista.
3. Confira os cartões por sentido: **Indo para Pau Serrado**, **Voltando para Shopping**, por exemplo. Compare com o destino do ônibus e com o lado da pista.
4. Cada cartão mostra a previsão no seu ponto e a saída prevista da parada anterior. **Pela tabela, já saiu** significa que a saída cadastrada passou; não é confirmação da posição real do veículo.
5. Sem GPS, use **Escolher ponto na pista pelo mapa**, toque no local e confirme em **Usar este ponto**. **Ver ponto associado na pista** mostra onde a previsão foi calculada.
6. Para conferir ambos os sentidos de uma parada cadastrada, selecione-a e toque em **Ver os dois sentidos desta parada**. **Ver horários da parada selecionada** volta à tabela tradicional.

Quando houver uma referência conhecida a até 150 m do ponto na pista, ela aparece como orientação. Os locais importados do OpenStreetMap não viram paradas oficiais. Sem uma referência próxima, o app mostra apenas o ponto da pista.

Os trajetos e tempos intermediários vêm do OSRM/OpenStreetMap. O cálculo pondera o progresso pelos tempos dos trechos da rua e usa os horários da linha para obter a passagem estimada. A rota rápida de carro entre pontos cadastrados pode diferir do itinerário real do ônibus. Não inclui trânsito ao vivo, atrasos ou rastreamento da frota.

O app associa apenas trajetos a até 200 m do ponto informado. GPS impreciso pode exigir seleção manual no mapa. Sem internet, usa a última tabela e os trajetos salvos. Se uma parada mudar de local, os trechos afetados são recalculados quando houver internet, usando apenas as coordenadas públicas das paradas. A localização do passageiro não é enviada ao serviço de roteamento nem publicada no banco.

Tabatinga tem o circuito calculado com os quatro pontos confirmados. Umarizeiras tem o trecho Rodoviária → Shopping; cadastrar sua origem habilita também os trechos até o bairro. Amanari depende de horários e coordenadas cadastrados.

Se aparecer uma solicitação do Play Protect para verificar o APK, permita a verificação e aguarde o resultado. Se houver aviso de app nocivo ou bloqueio, registre a mensagem e informe ao responsável pelo teste.

Validação: cálculos determinísticos, testes que detectam alterações intencionais nos cálculos, seleção e retorno do mapa, dois sentidos, estados de saída e GPS real no S21 FE. Os casos de horário foram verificados com hora de teste, sem simular que existam ônibus rastreados.

Dados cartográficos: © OpenStreetMap contributors (ODbL). Referências: [OSRM](https://project-osrm.org/docs/v5.24.0/api/), [Overpass](https://wiki.openstreetmap.org/wiki/Overpass_API).

