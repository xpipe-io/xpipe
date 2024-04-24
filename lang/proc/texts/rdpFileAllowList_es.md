# Integración de escritorio RDP

Puedes utilizar esta conexión RDP en XPipe para lanzar rápidamente aplicaciones y scripts. Sin embargo, debido a la naturaleza de RDP, tienes que editar la lista de aplicaciones remotas permitidas en tu servidor para que esto funcione. Además, esta opción permite compartir unidades para ejecutar tus scripts en tu servidor remoto.

También puedes optar por no hacer esto y simplemente utilizar XPipe para lanzar tu cliente RDP sin utilizar ninguna función avanzada de integración de escritorio.

## RDP permitir listas

Un servidor RDP utiliza el concepto de listas permitidas para gestionar el lanzamiento de aplicaciones. Esto significa esencialmente que, a menos que la lista de permitidas esté desactivada o que se hayan añadido explícitamente aplicaciones específicas a la lista de permitidas, el lanzamiento directo de cualquier aplicación remota fallará.

Puedes encontrar la configuración de la lista de permitidas en el registro de tu servidor en `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Permitir todas las aplicaciones

Puedes desactivar la lista de permitidas para permitir que todas las aplicaciones remotas se inicien directamente desde XPipe. Para ello, puedes ejecutar el siguiente comando en tu servidor en PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Añadir aplicaciones permitidas

También puedes añadir aplicaciones remotas individuales a la lista. Esto te permitirá lanzar las aplicaciones de la lista directamente desde XPipe.

En la clave `Aplicaciones` de `TSAppAllowList`, crea una nueva clave con un nombre arbitrario. El único requisito para el nombre es que sea único dentro de los hijos de la clave "Aplicaciones". Esta nueva clave debe contener los siguientes valores: `Nombre`, `Ruta` y `Configuración de la línea de comandos`. Puedes hacerlo en PowerShell con los siguientes comandos:

```
$appName="Bloc de notas"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
Nuevo-elemento -Ruta "$regKey\$appName"
Nuevo-elemento-Propiedad -Ruta "$regKey$$appName" -Nombre "Name" -Valor "$appName" -Force
Nueva-Propiedad-Artículo -Ruta "$regKey\$NombreDeLaAplicacion" -Nombre "Ruta" -Valor "$rutaDeLaAplicacion" -Forzar
Nuevo-Item-Propiedad -Ruta "$regKey\$NombreDeLaAplicacion" -Nombre "CommandLineSetting" -Valor "1" -PropertyType DWord -Force
<código>`</código

Si quieres permitir que XPipe ejecute también scripts y abra sesiones de terminal, tienes que añadir también `C:\Windows\System32\cmd.exe` a la lista de permitidos.

## Consideraciones de seguridad

Esto no hace que tu servidor sea inseguro en modo alguno, ya que siempre puedes ejecutar las mismas aplicaciones manualmente al iniciar una conexión RDP. Las listas de permitidos están más pensadas para evitar que los clientes ejecuten instantáneamente cualquier aplicación sin la intervención del usuario. A fin de cuentas, depende de ti si confías en XPipe para hacer esto. Puedes iniciar esta conexión sin problemas, sólo es útil si quieres utilizar alguna de las funciones avanzadas de integración de escritorio de XPipe.
