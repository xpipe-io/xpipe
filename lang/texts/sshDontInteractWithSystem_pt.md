## Deteção de tipo de shell

XPipe trabalha detectando o tipo de shell da conexão e então interage com o shell ativo. Esta abordagem só funciona, no entanto, quando o tipo de shell é conhecido e suporta uma certa quantidade de ações e comandos. Todos os shells comuns como `bash`, `cmd`, `powershell`, e mais, são suportados.

## Tipos de shell desconhecidos

Se estiveres a ligar a um sistema que não corre uma shell de comandos conhecida, por exemplo um router, link, ou algum dispositivo IOT, o XPipe será incapaz de detetar o tipo de shell e dará erro após algum tempo. Ao ativar esta opção, o XPipe não tentará identificar o tipo de shell e iniciará o shell como está. Isto permite-te abrir a ligação sem erros, mas muitas funcionalidades, por exemplo, o navegador de ficheiros, scripts, subconexões, etc., não serão suportadas para esta ligação.
