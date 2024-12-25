## Windows

En los sistemas Windows sueles referirte a los puertos serie mediante `COM<index>`.
XPipe también admite sólo especificar el índice sin el prefijo `COM`.
Para dirigirte a puertos mayores de 9, tienes que utilizar la forma de ruta UNC con `COM<index>`.

Si tienes instalada una distribución WSL1, también puedes hacer referencia a los puertos serie desde dentro de la distribución WSL mediante `/dev/ttyS<index>`.
Sin embargo, esto ya no funciona con WSL2.
Si tienes un sistema WSL1, puedes utilizarlo como host para esta conexión serie y utilizar la notación tty para acceder a él con XPipe.

## Linux

En los sistemas Linux normalmente puedes acceder a los puertos serie a través de `/dev/ttyS<index>`.
Si conoces el ID del dispositivo conectado pero no quieres seguir la pista del puerto serie, también puedes referenciarlos mediante `/dev/serial/by-id/<device id>`.
Puedes listar todos los puertos serie disponibles con sus ID ejecutando `ls /dev/serial/by-id/*`.

## macOS

En macOS, los nombres de los puertos serie pueden ser prácticamente cualquier cosa, pero suelen tener la forma de `/dev/tty.<id>` donde id es el identificador interno del dispositivo.
Ejecutando `ls /dev/tty.*` deberías encontrar los puertos serie disponibles.
