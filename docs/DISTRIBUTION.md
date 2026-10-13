# Distribuição dos passageiros

Downloads públicos: https://github.com/Angelosxzz/MeuOnibus-Downloads/releases/latest. Cada versão publicada fica na aba Releases, com notas e SHA-256. O repositório público contém apenas README e APK dos passageiros; não publique código, configuração, dados locais ou APK do administrador.

## Publicar uma versão

1. Atualize versionCode/versionName e release-config.json, compile e verifique assinatura, testes e instalação. Preserve a assinatura existente.
2. Execute node meu-onibus/build-app-release.mjs dentro da pasta meu-onibus (node build-app-release.mjs) e os checks do repositório.
3. Na raiz, execute node scripts/publish-passenger-release.mjs --publish. Usa a autenticação GitHub local em memória; não grava token. A publicação é idempotente e rejeita APK diferente para uma versão já publicada.
4. Confirme anonimamente o asset, SHA-256 e tamanho. Só depois publique a API com o manifesto da mesma versão.

O endereço compartilhado /downloads/Meu-Onibus.apk e a raiz do provedor redirecionam os navegadores à release mais recente. A API e o mapa continuam hospedados. Para manter compatibilidade com versões 0.5.0/0.6.0, o mesmo endpoint responde APK 200 quando Accept é application/vnd.android.package-archive, como enviado pelo atualizador instalado. Não redirecione esse fluxo: ele recusa redirects e confere hash e certificado.

Não é um agendamento automático: cada versão precisa ser compilada, verificada e publicada por esse fluxo. Nenhuma chave adicional do GitHub foi armazenada ou criada.
