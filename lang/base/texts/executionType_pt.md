## Tipos de execução

Existem dois tipos de execução distintos quando o XPipe se liga a um sistema.

### Em segundo plano

A primeira conexão com um sistema é feita em segundo plano em uma sessão de terminal burro.

Os comandos de bloqueio que requerem a entrada do usuário podem congelar o processo do shell quando o XPipe o inicia internamente primeiro em segundo plano. Para evitar isso, só deves chamar estes comandos de bloqueio no modo terminal.

O navegador de ficheiros, por exemplo, utiliza inteiramente o modo de fundo burro para tratar das suas operações, por isso, se quiseres que o teu ambiente de script se aplique à sessão do navegador de ficheiros, deve ser executado no modo burro.

### Nos terminais

Depois que a conexão inicial do terminal burro for bem-sucedida, o XPipe abrirá uma conexão separada no terminal real. Se quiseres que o script seja executado quando abrires a ligação num terminal, então escolhe o modo terminal.
