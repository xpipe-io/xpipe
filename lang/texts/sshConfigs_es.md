### SSH configs

XPipe carga todos los hosts y aplica todas las opciones que hayas configurado en el archivo seleccionado. Así, al especificar una opción de configuración global o específica de un host, se aplicará automáticamente a la conexión establecida por XPipe.

Si quieres saber más sobre cómo utilizar las configuraciones SSH, puedes utilizar `man ssh_config` o leer esta [guía](https://www.ssh.com/academy/ssh/config).

### Identidades

Ten en cuenta que aquí también puedes especificar una opción `IdentityFile`. Si se especifica alguna identidad aquí, se ignorará cualquier otra identidad especificada más abajo.

### Reenvío X11

Si se especifica aquí alguna opción para el reenvío X11, XPipe intentará configurar automáticamente el reenvío X11 en Windows a través de WSL.