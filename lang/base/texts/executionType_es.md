# Tipos de ejecución

Puedes utilizar un script en múltiples escenarios diferentes.

Al activar un script mediante su botón de activación, los tipos de ejecución dictan lo que XPipe hará con el script.

## Tipo de script de inicio

Cuando un script se designa como script init, se puede seleccionar en entornos shell para que se ejecute en init.

Además, si un script está activado, se ejecutará automáticamente en init en todos los shells compatibles.

Por ejemplo, si creas un script init simple con
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
tendrás acceso a estos alias en todas las sesiones de shell compatibles si el script está activado.

## Tipo de script ejecutable

Un script de shell ejecutable está destinado a ser llamado para una determinada conexión desde el concentrador de conexiones.
Cuando este script está habilitado, el script estará disponible para ser llamado desde el botón de scripts para una conexión con un dialecto shell compatible.

Por ejemplo, si creas un sencillo script de shell de dialecto `sh` llamado `ps` para mostrar la lista de procesos actuales con
```
ps -A
```
puedes llamar al script en cualquier conexión compatible en el menú scripts.

## Archivo tipo script

Por último, también puedes ejecutar scripts personalizados con entradas de archivo desde la interfaz del explorador de archivos.
Cuando se habilita un script de archivo, aparecerá en el explorador de archivos para ser ejecutado con entradas de archivo.

Por ejemplo, si creas un script de archivo simple con
```
diff "$1" "$2"
```
puedes ejecutar el script en los archivos seleccionados si el script está activado.
En este ejemplo, el script sólo se ejecutará correctamente si tienes exactamente dos archivos seleccionados.
De lo contrario, el comando diff fallará.

## Sesión de shell tipo script

Un script de sesión está pensado para ser llamado en una sesión shell en tu terminal.
Cuando está activado, el script se copiará en el sistema de destino y se pondrá en el PATH en todos los shells compatibles.
Esto te permite llamar al script desde cualquier lugar de una sesión de terminal.
El nombre del script se escribirá en minúsculas y los espacios se sustituirán por guiones bajos, lo que te permitirá llamarlo fácilmente.

Por ejemplo, si creas un script de shell sencillo para dialectos `sh` llamado `apti` con
```
sudo apt install "$1"
```
puedes llamar al script en cualquier sistema compatible con `apti.sh <pkg>` en una sesión de terminal si el script está activado.

## Tipos múltiples

También puedes marcar varias casillas para los tipos de ejecución de un script si deben utilizarse en varios escenarios.
