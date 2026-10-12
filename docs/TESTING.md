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
