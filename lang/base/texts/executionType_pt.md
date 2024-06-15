# Tipos de execução

Podes utilizar um script em vários cenários diferentes.

Ao ativar um script, os tipos de execução ditam o que o XPipe fará com o script.

## Scripts de inicialização

Quando um script é designado como script de inicialização, ele pode ser selecionado em ambientes shell.

Além disso, se um script é habilitado, ele será automaticamente executado no init em todos os shells compatíveis.

Por exemplo, se criares um script de inicialização simples como
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
terás acesso a estes aliases em todas as sessões de shell compatíveis se o script estiver ativado.

## Scripts de shell

Um script de shell normal destina-se a ser chamado numa sessão de shell no teu terminal.
Quando ativado, o script será copiado para o sistema alvo e colocado no PATH em todas as shells compatíveis.
Isto permite-te chamar o script a partir de qualquer lugar numa sessão de terminal.
O nome do script será escrito em minúsculas e os espaços serão substituídos por sublinhados, permitindo-te chamar facilmente o script.

Por exemplo, se criares um script de shell simples chamado `apti` como
```
sudo apt install "$1"
```
podes chamar isso em qualquer sistema compatível com `apti.sh <pkg>` se o script estiver ativado.

## Scripts de ficheiros

Por último, também podes executar scripts personalizados com entradas de ficheiros a partir da interface do navegador de ficheiros.
Quando um script de arquivo estiver habilitado, ele aparecerá no navegador de arquivos para ser executado com entradas de arquivo.

Por exemplo, se criares um script de arquivo simples como
```
sudo apt install "$@"
```
podes executar o script em ficheiros seleccionados se o script estiver ativado.

## Vários tipos

Como o script de arquivo de exemplo é o mesmo que o script de shell de exemplo acima,
vês que também podes assinalar várias caixas para os tipos de execução de um script, se estes tiverem de ser usados em vários cenários.


