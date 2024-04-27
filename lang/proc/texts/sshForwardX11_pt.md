## X11 Forwarding

Quando esta opção é ativada, a conexão SSH será iniciada com o encaminhamento X11 configurado. No Linux, isso geralmente funcionará fora da caixa e não requer nenhuma configuração. No macOS, precisas de um servidor X11 como o [XQuartz](https://www.xquartz.org/) a correr na tua máquina local.

### X11 no Windows

XPipe permite-te usar as capacidades X11 do WSL2 para a tua ligação SSH. A única coisa que precisas para isto é de uma distribuição [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) instalada no teu sistema local. XPipe escolherá automaticamente uma distribuição compatível instalada, se possível, mas também podes usar outra no menu de configurações.

Isto significa que não precisas de instalar um servidor X11 separado no Windows. No entanto, se estiveres a utilizar um, o XPipe irá detectá-lo e utilizar o servidor X11 atualmente em execução.

### Conexões X11 como desktops

Qualquer conexão SSH que tenha o encaminhamento X11 habilitado pode ser usada como um host de desktop. Isto significa que podes lançar aplicações desktop e ambientes desktop através desta ligação. Quando qualquer aplicação desktop é lançada, esta conexão será automaticamente iniciada em segundo plano para iniciar o túnel X11.
