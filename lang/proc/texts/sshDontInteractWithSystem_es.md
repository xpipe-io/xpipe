## Detección del tipo de shell

XPipe funciona detectando el tipo de shell de la conexión y luego interactuando con el shell activo. Sin embargo, este método sólo funciona cuando se conoce el tipo de shell y admite un cierto número de acciones y comandos. Todos los shells comunes como `bash`, `cmd`, `powershell`, y más, son compatibles.

## Tipos de shell desconocidos

Si te conectas a un sistema que no ejecuta un shell de comandos conocido, por ejemplo, un router, un enlace o algún dispositivo IOT, XPipe será incapaz de detectar el tipo de shell y dará error al cabo de un tiempo. Activando esta opción, XPipe no intentará identificar el tipo de shell y lanzará el shell tal cual. Esto te permite abrir la conexión sin errores, pero muchas funciones, como el explorador de archivos, los scripts, las subconexiones, etc., no serán compatibles con esta conexión.
