## Ligações shell personalizadas

Abre um shell usando o comando personalizado, executando o comando dado no sistema host selecionado. Esta shell pode ser local ou remota.

Observa que essa funcionalidade espera que o shell seja de um tipo padrão, como `cmd`, `bash`, etc. Se quiseres abrir quaisquer outros tipos de shells e comandos num terminal, podes utilizar o tipo de comando de terminal personalizado. A utilização de shells padrão permite-te também abrir esta ligação no navegador de ficheiros.

### Prompts interactivos

O processo do shell pode expirar ou travar no caso de haver um prompt de entrada
inesperado, como um prompt de senha. Portanto, deves sempre certificar-te de que não há prompts de entrada interativos.

Por exemplo, um comando como `ssh user@host` funcionará bem aqui, desde que não seja necessária uma senha.

### Shells locais personalizados

Em muitos casos, é útil iniciar um shell com certas opções que geralmente são desativadas por padrão, a fim de fazer alguns scripts e comandos funcionarem corretamente. Por exemplo:

-   [Expansão atrasada em
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Execução do Powershell
    políticas](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- E qualquer outra opção de lançamento possível para um shell de sua escolha

Isto pode ser conseguido através da criação de comandos de shell personalizados com, por exemplo, os seguintes comandos:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`