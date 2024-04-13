## Conexiones shell personalizadas

Abre un shell utilizando el comando personalizado ejecutando el comando dado en el sistema anfitrión seleccionado. Este shell puede ser local o remoto.

Ten en cuenta que esta funcionalidad espera que el intérprete de comandos sea de tipo estándar, como `cmd`, `bash`, etc. Si quieres abrir cualquier otro tipo de shell y comandos en un terminal, puedes utilizar en su lugar el tipo de comando terminal personalizado. Si utilizas shells estándar, también podrás abrir esta conexión en el explorador de archivos.

### Avisos interactivos

El proceso del intérprete de comandos puede agotarse o bloquearse en caso de que se solicite una entrada inesperada, como una contraseña
inesperada, como una solicitud de contraseña. Por lo tanto, debes asegurarte siempre de que no hay peticiones de entrada interactivas.

Por ejemplo, un comando como `ssh usuario@host` funcionará bien siempre que no se solicite una contraseña.

### Shell local personalizado

En muchos casos, es útil lanzar un intérprete de comandos con determinadas opciones que suelen estar desactivadas por defecto para que algunos scripts y comandos funcionen correctamente. Por ejemplo

-   [Expansión retardada en
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Ejecución de Powershell
    políticas](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Modo](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Y cualquier otra opción de lanzamiento posible para un intérprete de comandos de tu elección

Esto se puede conseguir creando comandos de shell personalizados con, por ejemplo, los siguientes comandos:

-   <code>cmd /v</code
-   <código>powershell -ModoEjecución Bypass</código
-   <código>bash --posix</código