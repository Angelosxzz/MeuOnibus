# Desenvolvimento

## Ambiente

Use JDK 17 e Node 24+. Instale pelo SDK Manager a plataforma Android 36 e Build Tools que forneçam aapt2, D8, zipalign e apksigner. Aceite os termos do SDK na sua própria instalação.

Abra cada pasta Android separadamente no Android Studio. Cada projeto inclui Gradle Wrapper 8.11.1, com checksum da distribuição e JAR conferidos na fonte oficial. No Windows rode ./gradlew.bat assembleDebug dentro da pasta do app; no Linux/macOS rode ./gradlew assembleDebug. O build Gradle usa Build Tools 36.0.0. O método PowerShell abaixo também está disponível.

## Compilar no Windows

```powershell
./meu-onibus-android/build-apk.ps1 -Jdk 'C:/jdk-17' -Platform 'C:/Android/Sdk/platforms/android-36' -BuildTools 'C:/Android/Sdk/build-tools/36.0.0' -Keystore 'C:/chaves/meu-onibus-development.keystore'
./meu-onibus-admin/build-apk.ps1 -Jdk 'C:/jdk-17' -Platform 'C:/Android/Sdk/platforms/android-36' -BuildTools 'C:/Android/Sdk/build-tools/36.0.0' -Keystore 'C:/chaves/meu-onibus-development.keystore'
```

Os scripts atuais usam o alias e senha padrão de desenvolvimento Android e criam uma chave caso o caminho não exista. Não use esse procedimento para criar uma nova identidade de assinatura de produção. A chave das instalações atuais deve ficar protegida fora do repositório; uma chave diferente impede atualizar o app existente.

## API

Não há dependências npm de execução. Rode os testes com `npm test` dentro de `meu-onibus`. `npm run build` gera `dist/server/index.js` para empacotamento. A publicação exige um runtime compatível com Worker e um banco D1 no binding `DB`; o segredo `ADMIN_KEY` deve ser definido no ambiente do provedor.

Os apps e a política de atualização ainda apontam para o servidor atual. Ao migrar o domínio, revise CloudDatabase, MapPickerActivity, UpdatePolicy, build-app-release e testes dos dois apps. Nunca permita qualquer host arbitrário no instalador.

O script de build da cópia GitHub não copia identificação privada do provedor de hospedagem. A configuração da publicação deve ser fornecida no ambiente, separadamente.
