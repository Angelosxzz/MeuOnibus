# Meu Ônibus

Aplicativo Android e versão web para consultar previsões de passagem em Maranguape, com horários compartilhados na nuvem.

**Web para iPhone e Android:** [abrir Meu Ônibus](https://meu-onibus.pages.dev/web/). No Safari, use Compartilhar → Adicionar à Tela de Início. A sincronização da nova hospedagem ainda está em correção; a página informa quando utiliza a tabela salva.

**Passageiros:** versão 0.6.2 · **Android mínimo:** 8.0 (API 26).

As previsões são calculadas a partir das tabelas e do percurso. Não há rastreamento por GPS dos ônibus. Tabatinga inclui simulações a cada 8 minutos; Amanari ainda não tem tabela confirmada.

## Componentes

| Pasta | Responsabilidade |
| --- | --- |
| [meu-onibus-android](meu-onibus-android) | App dos passageiros, GPS, mapa, ida/volta e atualizações |
| [meu-onibus](meu-onibus) | API Worker, migrações SQLite/D1 e distribuição do APK |
| [meu-onibus/web](meu-onibus/web) | Interface web, mapa, sugestões e instalação na tela inicial |
| [docs](docs) | Arquitetura, instalação, testes e operação |

## Instalar e testar

O APK dos passageiros está em [Meu Ônibus](meu-onibus-android/Meu-Onibus.apk). O código e o APK do administrador são mantidos separadamente, fora deste repositório. O Android pode solicitar autorização para instalar aplicativos desta fonte; não desative o Play Protect.

Veja [instalação e uso](Como-testar-Meu-Onibus.md), [desenvolvimento](docs/DEVELOPMENT.md) e [testes](docs/TESTING.md).

## Começar a desenvolver

Requisitos: JDK 17, Node.js 24+, Android SDK com plataforma 36 e Build Tools compatíveis. O projeto meu-onibus-android pode ser aberto no Android Studio. O caminho de compilação já verificado é o script PowerShell de cada app.

```powershell
./scripts/check.ps1 -Jdk 'C:/caminho/jdk-17'
```

Esse comando executa previsões, política de atualização e API com banco SQLite em memória. Nenhuma alteração é enviada ao banco de produção.

## Manutenção

- [Arquitetura e fluxo de dados](docs/ARCHITECTURE.md)
- [API e autenticação](docs/API.md)
- [Versão web e limites dos testes](docs/WEB.md) · [Publicação na Cloudflare](docs/CLOUDFLARE.md)
- [Publicar uma versão e recuperar falhas](docs/RELEASING.md)
- [Privacidade e segurança](SECURITY.md)
- [Contribuir](CONTRIBUTING.md) · [Histórico](CHANGELOG.md) · [Próximos passos](docs/ROADMAP.md)

O repositório é privado. Senhas, tokens, chave administrativa, certificados privados de assinatura e localização pessoal não fazem parte dele. Os APKs atuais usam uma chave de desenvolvimento preservada fora do Git; sua migração para assinatura de produção precisa manter a compatibilidade com as instalações existentes.

## Referências

A organização dos guias e da validação usa como referências [Now in Android](https://github.com/android/nowinandroid), [Architecture Samples](https://github.com/android/architecture-samples) e [Testing Samples](https://github.com/android/testing-samples). O código atual permanece Java com Android Views, sem copiar a arquitetura ou as dependências dessas aplicações.

Os mapas usam OpenStreetMap/Leaflet. Consulte as atribuições em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
