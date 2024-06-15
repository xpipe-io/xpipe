# Tipos de ejecución

Puedes utilizar un script en múltiples escenarios diferentes.

Al activar un script, los tipos de ejecución dictan lo que XPipe hará con el script.

## Guiones de inicio

Cuando un script se designa como script init, se puede seleccionar en entornos shell.

Además, si un script está activado, se ejecutará automáticamente en init en todos los shells compatibles.

Por ejemplo, si creas un script init sencillo como
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
tendrás acceso a estos alias en todas las sesiones de shell compatibles si el script está activado.

## Scripts de shell

Un script de shell normal está pensado para ser llamado en una sesión de shell en tu terminal.
Cuando está activado, el script se copiará en el sistema de destino y se pondrá en el PATH en todas las shell compatibles.
Esto te permite llamar al script desde cualquier lugar de una sesión de terminal.
El nombre del script se escribirá en minúsculas y los espacios se sustituirán por guiones bajos, lo que te permitirá llamarlo fácilmente.

Por ejemplo, si creas un sencillo script de shell llamado `apti` como
```
sudo apt install "$1"
```
puedes invocarlo en cualquier sistema compatible con `apti.sh <pkg>` si el script está activado.

## Archivo scripts

Por último, también puedes ejecutar scripts personalizados con entradas de archivo desde la interfaz del explorador de archivos.
Cuando un script de archivo esté activado, aparecerá en el explorador de archivos para ejecutarse con entradas de archivo.

Por ejemplo, si creas un script de archivo sencillo como
```
sudo apt install "$@"
```
puedes ejecutar el script en los archivos seleccionados si el script está activado.

## Tipos múltiples

Como el script de archivo de ejemplo es el mismo que el script de shell de ejemplo anterior,
verás que también puedes marcar varias casillas para los tipos de ejecución de un script si deben utilizarse en varios escenarios.


