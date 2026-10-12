# Segurança e privacidade

Este repositório é privado e não contém a chave administrativa, tokens de serviço, chave privada de assinatura, fotos pessoais nem capturas com a localização do proprietário. Não envie esses arquivos em issues ou pull requests.

O segredo ADMIN_KEY pertence ao ambiente do servidor. O app de passageiros não recebe acesso administrativo. O app admin guarda a chave apenas em memória durante a sessão. Nunca inclua valores reais em exemplos, logs, commits ou APKs.

Sugestões com coordenadas são dados enviados ao servidor e devem ser vistas apenas pelo administrador. Os dados publicados das paradas e trajetos são informações de transporte. O GPS para cálculo fica no aparelho; tiles do mapa são servidos por terceiros.

Os APKs atuais têm assinatura de desenvolvimento. A chave privada foi excluída; proteja o arquivo original e planeje assinatura de produção antes de distribuição ampla. O segredo de desenvolvimento padrão não equivale a uma chave de produção protegida.

Para relatar vulnerabilidades, fale diretamente com o mantenedor por um canal privado já conhecido, descrevendo reprodução sem credenciais ou coordenadas pessoais. Não abra uma issue pública. Em vazamento, revogue o segredo no provedor e remova-o também do histórico; apagar só o arquivo atual não elimina versões anteriores.
