### Não tens nada

Desativa a autenticação `publickey`.

### SSH-Agent

Caso as tuas identidades estejam armazenadas no SSH-Agent, o executável ssh pode usá-las se o agente for iniciado.
O XPipe iniciará automaticamente o processo do agente se ele ainda não estiver em execução.

### Pageant (Windows)

Caso estejas a utilizar o pageant no Windows, o XPipe irá verificar se o pageant está a ser executado primeiro.
Devido à natureza do pageant, é da tua responsabilidade tê-lo
a responsabilidade de o ter em execução, uma vez que tens de especificar manualmente todas as chaves que gostarias de adicionar de cada vez.
Se estiver em execução, o XPipe passará o pipe nomeado apropriado via
`-oIdentityAgent=...` para o ssh, não tens de incluir quaisquer ficheiros de configuração personalizados.

Nota que existem alguns bugs de implementação no cliente OpenSSH que podem causar problemas
se o seu nome de usuário contiver espaços ou for muito longo, então tenta usar a versão mais recente.

### Pageant (Linux & macOS)

Caso as tuas identidades estejam armazenadas no agente pageant, o executável ssh pode usá-las se o agente for iniciado.
O XPipe iniciará automaticamente o processo do agente se ele ainda não estiver em execução.

### Arquivo de identidade

Também podes especificar um ficheiro de identidade com uma frase-chave opcional.
Esta opção é o equivalente a `ssh -i <file>`.

Nota que esta deve ser a chave *privada*, não a pública.
Se misturares isso, o ssh apenas te dará mensagens de erro crípticas.

### Agente GPG

Se as tuas identidades estão armazenadas, por exemplo, num smartcard, podes escolher fornecê-las ao cliente SSH através do `gpg-agent`.
Esta opção habilitará automaticamente o suporte SSH do agente se ainda não estiver habilitado e reiniciará o daemon do agente GPG com as configurações corretas.

### Yubikey PIV

Se as tuas identidades estão armazenadas com a função de cartão inteligente PIV do Yubikey, podes recuperá-las
podes recuperá-las com a biblioteca YKCS11 do Yubico, que vem junto com a ferramenta Yubico PIV.

Nota que necessita de uma versão actualizada do OpenSSH para poder utilizar esta função.

### Agente personalizado

Também podes usar um agente personalizado fornecendo a localização do socket ou a localização do pipe nomeado aqui.
Isto irá passá-lo através da opção `IdentityAgent`.

### Biblioteca PKCS#11 personalizada

Isso instrui o cliente OpenSSH a carregar o arquivo de biblioteca compartilhada especificado, que irá lidar com a autenticação.

Nota que precisas de uma versão actualizada do OpenSSH para usar esta funcionalidade.
