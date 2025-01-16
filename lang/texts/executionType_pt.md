# Tipos de execução

Podes utilizar um script em vários cenários diferentes.

Ao ativar um script através do respetivo botão de alternância, os tipos de execução determinam o que o XPipe fará com o script.

## Tipo de script de inicialização

Quando um script é designado como script init, ele pode ser selecionado em ambientes shell para ser executado no init.

Além disso, se um script é habilitado, ele será automaticamente executado no init em todos os shells compatíveis.

Por exemplo, se criares um script init simples com
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
terás acesso a estes aliases em todas as sessões de shell compatíveis se o script estiver ativado.

## Tipo de script executável

Um script de shell executável destina-se a ser chamado para uma determinada ligação a partir do hub de ligação.
Quando este script está ativado, o script estará disponível para ser chamado a partir do botão de scripts para uma ligação com um dialeto de shell compatível.

Por exemplo, se você criar um script de shell de dialeto `sh` simples chamado `ps` para mostrar a lista de processos atual com
```
ps -A
```
podes chamar o script em qualquer conexão compatível no menu de scripts.

## Tipo de script de arquivo

Por fim, também podes executar um script personalizado com entradas de ficheiro a partir da interface do navegador de ficheiros.
Quando um script de ficheiro está ativado, aparece no navegador de ficheiros para ser executado com entradas de ficheiro.

Por exemplo, se criares um script de ficheiro simples com
```
diff "$1" "$2"
```
podes executar o script em ficheiros seleccionados se o script estiver ativado.
Neste exemplo, o script só será executado com êxito se tiveres exatamente dois arquivos selecionados.
Caso contrário, o comando diff falhará.

## Tipo de script da sessão do shell

Um script de sessão destina-se a ser chamado numa sessão de shell no teu terminal.
Quando ativado, o script será copiado para o sistema alvo e colocado no PATH em todas as shells compatíveis.
Isto permite-te chamar o script a partir de qualquer lugar numa sessão de terminal.
O nome do script será escrito em minúsculas e os espaços serão substituídos por sublinhados, permitindo-te chamar facilmente o script.

Por exemplo, se você criar um script de shell simples para dialetos `sh` chamado `apti` com
```
sudo apt instala "$1"
```
podes chamar o script em qualquer sistema compatível com `apti.sh <pkg>` numa sessão de terminal se o script estiver ativado.

## Múltiplos tipos

Também podes assinalar várias caixas para os tipos de execução de um script se estes tiverem de ser utilizados em vários cenários.
