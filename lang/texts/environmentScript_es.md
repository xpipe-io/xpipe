## Guión de inicio

Los comandos opcionales que se ejecutarán después de que se hayan ejecutado los archivos y perfiles init del intérprete de comandos.

Puedes tratarlo como un script de shell normal, es decir, hacer uso de toda la sintaxis que el shell admite en los scripts. Todos los comandos que ejecutes tienen su origen en el shell y modifican el entorno. Así que si, por ejemplo, estableces una variable, tendrás acceso a esta variable en esta sesión de shell.

### Comandos de bloqueo

Ten en cuenta que los comandos de bloqueo que requieren la entrada del usuario pueden congelar el proceso del shell cuando XPipe lo inicie internamente primero en segundo plano. Para evitarlo, sólo llama a estos comandos de bloqueo si la variable `TERM` no está establecida a `tonto`. XPipe establece automáticamente la variable `TERM=dumb` cuando prepara la sesión shell en segundo plano y luego establece `TERM=xterm-256color` cuando abre realmente el terminal.