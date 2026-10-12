# Testes

Execute `./scripts/check.ps1 -Jdk 'C:/jdk-17'` na raiz. Java 17 e Node 24+ são necessários. O workflow CI executa essas mesmas verificações e compila ambos os apps em modo debug, sem credenciais de produção. As actions estão fixadas por SHA.

| Verificação | Evidência |
| --- | --- |
| RoadForecastCheck | Projeção, sentidos, interpolação temporal e passagem após a saída |
| UpdatePolicyCheck | Versões iguais/antigas, host, tamanho, pacote e checksum inválidos |
| check-suggestions.mjs | SQLite real: autenticação, pausa, aprovação, rejeição, duplicação e limites |
| check-app-release.mjs | APK exato, SHA-256, MIME, tamanho, HEAD e restrição de métodos |

Os testes de backend criam um SQLite em memória com as migrações. Não contatam nem alteram produção. A chave test-only é exclusivamente uma fixture de teste.

## Aparelho por ADB

```powershell
adb devices
adb install -r meu-onibus-android/Meu-Onibus.apk
adb shell am start -n br.com.meuonibus/.MainActivity
adb shell dumpsys package br.com.meuonibus
```

Não desinstale um app para resolver erro de assinatura sem salvar os dados necessários. Antes de compartilhar um APK, verifique sua assinatura com apksigner e seu SHA-256.

Validação anterior no S21 FE: seleção de ponto no mapa, localização, previsões nos dois sentidos, download validado e abertura da autorização de instalação. A versão 0.5.0 foi instalada. Os testes de instrumentação temporários usados nessa validação ainda não foram incorporados a uma suíte reproduzível no repositório. Não há evidência de teste em todos os modelos Android.

Checklist manual de regressão: consultar sem internet; negar GPS e escolher ponto manualmente; abrir tabela; enviar proposta sem alterar dado oficial; revisar pelo admin; pausar sugestões; verificar que versão igual não oferece atualização. Não envie propostas fictícias ao banco de produção.

Compilação local com Gradle Wrapper 8.11.1: assembleDebug passou para os dois apps. Os APKs debug do CI têm assinatura de teste e não substituem os APKs de distribuição existentes.

## Regressões da auditoria de 12/10/2026

`check-audit.mjs` integra `npm test` e `scripts/check.ps1`: erros assíncronos controlados, indisponibilidade do banco, ETag/304 e mudança de revisão, limite em bytes UTF-8, interrupção de corpo grande e CSP/SRI do mapa. Usa SQLite em memória. Cinco verificações falharam antes das correções; depois, as seis passaram.

A instrumentação versionada `AuditChecks` usa SQLite real e verifica validação do cache, revisão fracionária, latitude inválida, HTTP 304, seleção do mapa após recriação e troca de parada sem toque. Exige pacote `.audit` e endpoint local; não executa no app dos usuários. As cinco primeiras reproduziram falhas na base; a sexta precisou de ajuste na fixture. A suíte final foi compilada, mas não foi executada após as correções: o aparelho deixou de aparecer no ADB. Não considerar esses fluxos aprovados em dispositivo.

Para repetir, em terminais separados:

```powershell
# Na raiz do repositório; servidor somente em loopback e banco em memória
node meu-onibus/local-audit-server.mjs
node scripts/prepare-audit-fixture.mjs ../meu-onibus-qa
adb reverse tcp:8187 tcp:8187
# Na cópia QA: JDK 17 e SDK 36 configurados
./meu-onibus-android/gradlew.bat -p meu-onibus-android assembleDebug assembleDebugAndroidTest lintDebug
adb install -r meu-onibus-android/app/build/outputs/apk/debug/app-debug.apk
adb install -r meu-onibus-android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w br.com.meuonibus.audit.test/br.com.meuonibus.AuditChecks
```

A saída deve conter `audit=PASS` e zero falhas; código de saída do adb sozinho não prova sucesso. O mapa ainda exige internet e consulta a página pública atual, usando coordenadas públicas de teste. Não testa a publicação do novo HTML em produção. A chave do servidor de teste é `test-only`. Ao terminar, pare o servidor, remova o reverse 8187 e, se desejado, desinstale apenas os pacotes `.audit` e `.audit.test`. Não desinstale os apps originais.

MapUrlPolicyCheck também integra a verificação Java: permite o mapa oficial e rejeita host impostor, HTTP, userinfo, porta externa, caminho diferente/encodado e esquemas não HTTPS. A remoção da guarda de userinfo em uma cópia temporária produziu a falha esperada. Compilações finais: Gradle/lint e PowerShell dos dois apps aprovadas; lint com 38/29 avisos e zero erros. Os APKs de fixture apontam à API local e não devem ser compartilhados.
