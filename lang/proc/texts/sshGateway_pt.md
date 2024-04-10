## Gateways de ligação Shell

Se ativado, o XPipe primeiro abre uma conexão shell para o gateway e, a partir daí, abre uma conexão SSH para o host especificado. O comando `ssh` deve estar disponível e localizado no `PATH` no gateway escolhido.

### Salta para os servidores

Este mecanismo é semelhante aos servidores de salto, mas não é equivalente. É completamente independente do protocolo SSH, então você pode usar qualquer conexão shell como um gateway.

Se estás à procura de servidores de salto SSH adequados, talvez também em combinação com o encaminhamento de agentes, usa a funcionalidade de ligação SSH personalizada com a opção de configuração `ProxyJump`.