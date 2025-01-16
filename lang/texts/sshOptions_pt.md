## Configurações SSH

Aqui podes especificar quaisquer opções SSH que devem ser passadas para a ligação.
Enquanto algumas opções são essencialmente necessárias para estabelecer uma conexão com sucesso, como `HostName`,
muitas outras opções são puramente opcionais.

Para ter uma visão geral de todas as opções possíveis, podes usar [`man ssh_config`](https://linux.die.net/man/5/ssh_config) ou ler este [guia](https://www.ssh.com/academy/ssh/config).
A quantidade exacta de opções suportadas depende puramente do teu cliente SSH instalado.

### Formatação

O conteúdo aqui é equivalente a uma seção de host em um arquivo de configuração SSH.
Nota que não tens de definir explicitamente a chave `Host`, pois isso será feito automaticamente.

Se você pretende definir mais de uma seção de host, por exemplo, com conexões dependentes, como um host de salto de proxy que depende de outro host de configuração, você pode definir várias entradas de host aqui também. O XPipe lançará então a primeira entrada de host.

Não tens de fazer qualquer formatação com espaços em branco ou indentação, não é necessário para que funcione.

Nota que deves ter o cuidado de colocar aspas nos valores se estes contiverem espaços, caso contrário serão passados incorretamente.

### Ficheiros de identidade

Nota que também podes especificar uma opção `IdentityFile` aqui.
Se esta opção for especificada aqui, qualquer opção de autenticação baseada em chave especificada mais abaixo será ignorada.

Se escolheres fazer referência a um ficheiro de identidade que é gerido no XPipe git vault, também o podes fazer.
O XPipe irá detetar ficheiros de identidade partilhados e adaptar automaticamente o caminho do ficheiro em cada sistema em que clonaste o git vault.
