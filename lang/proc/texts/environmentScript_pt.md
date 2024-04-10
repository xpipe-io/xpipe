## Script de inicialização

Os comandos opcionais a serem executados após os arquivos e perfis de inicialização do shell terem sido executados.

Podes tratar isto como um script de shell normal, i.e. fazer uso de toda a sintaxe que a shell suporta em scripts. Todos os comandos que executas são originados pela shell e modificam o ambiente. Assim, se por exemplo definires uma variável, terás acesso a esta variável nesta sessão da shell.

### Comandos de bloqueio

Nota que os comandos de bloqueio que requerem a entrada do utilizador podem congelar o processo da shell quando o XPipe o inicia internamente primeiro em segundo plano. Para evitar isso, só chama esses comandos de bloqueio se a variável `TERM` não estiver definida como `dumb`. O XPipe define automaticamente a variável `TERM=dumb` quando está preparando a sessão do shell em segundo plano e, em seguida, define `TERM=xterm-256color` quando realmente abre o terminal.