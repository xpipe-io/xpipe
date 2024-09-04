### Não tens nada

Se selecionado, o XPipe não fornecerá quaisquer identidades. Isto também desactiva quaisquer fontes externas como agentes.

### Ficheiro de identidade

Podes também especificar um ficheiro de identidade com uma frase-chave opcional.
Esta opção é o equivalente a `ssh -i <file>`.

Nota que esta deve ser a chave *privada*, não a pública.
Se misturares isso, o ssh apenas te dará mensagens de erro crípticas.

### Agente SSH

Caso as tuas identidades estejam armazenadas no SSH-Agent, o executável ssh pode usá-las se o agente for iniciado.
O XPipe iniciará automaticamente o processo do agente se ele ainda não estiver em execução.

### Agente GPG

Se as tuas identidades estão armazenadas, por exemplo, num smartcard, podes escolher fornecê-las ao cliente SSH através do `agente GPG`.
Esta opção habilitará automaticamente o suporte SSH do agente se ainda não estiver habilitado e reiniciará o daemon do agente GPG com as configurações corretas.

### Yubikey PIV

Se as tuas identidades estão armazenadas com a função de cartão inteligente PIV do Yubikey, podes recuperá-las
podes recuperá-las com a biblioteca YKCS11 do Yubico, que vem junto com a ferramenta Yubico PIV.

Nota que necessita de uma versão actualizada do OpenSSH para poder utilizar esta função.

### Biblioteca PKCS#11 personalizada

Isso instruirá o cliente OpenSSH a carregar o arquivo de biblioteca compartilhada especificado, que lidará com a autenticação.

Nota que precisas de uma versão actualizada do OpenSSH para usar esta funcionalidade.

### Pageant (Windows)

Caso estejas a usar o pageant no Windows, o XPipe irá verificar se o pageant está a ser executado primeiro.
Devido à natureza do pageant, é da tua responsabilidade tê-lo
a responsabilidade de o ter em execução, uma vez que tens de especificar manualmente todas as chaves que gostarias de adicionar de cada vez.
Se estiver em execução, o XPipe passará o pipe nomeado apropriado via
`-oIdentityAgent=...` para o ssh, não tens de incluir quaisquer ficheiros de configuração personalizados.

### Pageant (Linux & macOS)

Caso as tuas identidades estejam armazenadas no agente pageant, o executável ssh pode usá-las se o agente for iniciado.
O XPipe iniciará automaticamente o processo do agente se ele ainda não estiver em execução.

### Outra fonte externa

Esta opção permitirá que qualquer provedor de identidade externo em execução forneça suas chaves para o cliente SSH. Deves utilizar esta opção se estiveres a utilizar qualquer outro agente ou gestor de palavras-passe para gerir as tuas chaves SSH.
