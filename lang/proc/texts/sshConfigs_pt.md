### Configurações SSH

O XPipe carrega todos os hosts e aplica todas as configurações que configuraste no ficheiro selecionado. Assim, ao especificares uma opção de configuração numa base global ou específica do host, esta será automaticamente aplicada à ligação estabelecida pelo XPipe.

Se quiseres aprender mais sobre como usar as configurações SSH, podes usar `man ssh_config` ou ler este [guia] (https://www.ssh.com/academy/ssh/config).

### Identidades

Nota que também podes especificar uma opção `IdentityFile` aqui. Se qualquer identidade é especificada aqui, qualquer outra identidade especificada mais abaixo será ignorada.

### Encaminhamento X11

Se qualquer opção para encaminhamento X11 for especificada aqui, o XPipe tentará automaticamente configurar o encaminhamento X11 no Windows através do WSL.