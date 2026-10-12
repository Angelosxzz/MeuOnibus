# Publicação e recuperação

1. Execute todos os testes e registre a revisão dos dados em produção. Faça backup do D1 pelo provedor em local privado.
2. Altere versionCode e versionName no Manifest e app/build.gradle; versionCode deve crescer. Atualize release-config.json e CHANGELOG.
3. Compile com a mesma chave da versão instalada. Confira pacote, versão e assinatura. Teste no aparelho com adb install -r.
4. Dentro de meu-onibus execute npm run build:release, npm test e npm run build. O gerador incorpora o APK validado ao Worker com tamanho e SHA-256.
5. Publique API e APK juntos no provedor atual, preservando DB e ADMIN_KEY. Nunca substitua os dados atuais pela seed na atualização.
6. Confira /api/app-release e compare o hash do download com o arquivo local. Um app com a versão anterior deve oferecer atualização; a versão atual não deve oferecer.

GitHub armazena o código. O banco continua no provedor atual; GitHub Pages não executa Worker nem D1. Esta cópia privada não oferece downloads anônimos aos amigos. Compartilhe apenas o APK dos passageiros pelo canal de distribuição vigente.

## Recuperar uma falha

Em falha de API, reverta para a versão anterior do Worker pelo provedor, preservando o banco e os segredos. Não apague nem reaplique migrações já executadas. Para dados incorretos, restaure um backup validado ou corrija pelo administrador, registrando a nova revisão.

Android não aceita normalmente downgrade de versionCode. Corrija o app em uma nova versão com código superior; não oriente usuários a remover o app como procedimento normal. A chave de assinatura deve ter backup privado, separado do código.
