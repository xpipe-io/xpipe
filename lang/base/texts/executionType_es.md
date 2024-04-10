## Tipos de ejecución

Hay dos tipos de ejecución distintos cuando XPipe se conecta a un sistema.

### En segundo plano

La primera conexión a un sistema se realiza en segundo plano en una sesión de terminal tonta.

Los comandos de bloqueo que requieren la entrada del usuario pueden congelar el proceso shell cuando XPipe lo inicia internamente por primera vez en segundo plano. Para evitarlo, sólo debes llamar a estos comandos de bloqueo en el modo terminal.

El explorador de archivos, por ejemplo, utiliza enteramente el modo mudo en segundo plano para manejar sus operaciones, así que si quieres que tu entorno de script se aplique a la sesión del explorador de archivos, debe ejecutarse en el modo mudo.

### En los terminales

Después de que la conexión inicial de terminal mudo haya tenido éxito, XPipe abrirá una conexión separada en el terminal real. Si quieres que el script se ejecute al abrir la conexión en un terminal, elige el modo terminal.
