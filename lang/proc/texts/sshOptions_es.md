## Configuraciones SSH

Aquí puedes especificar cualquier opción SSH que deba pasarse a la conexión.
Aunque algunas opciones son esencialmente necesarias para establecer con éxito una conexión, como `HostName`,
muchas otras opciones son puramente opcionales.

Para obtener una visión general de todas las opciones posibles, puedes utilizar [`man ssh_config`](https://linux.die.net/man/5/ssh_config) o leer esta [guía](https://www.ssh.com/academy/ssh/config).
La cantidad exacta de opciones soportadas depende puramente de tu cliente SSH instalado.

### Formato

El contenido aquí es equivalente a una sección de host en un archivo de configuración SSH.
Ten en cuenta que no tienes que definir explícitamente la clave `Host`, ya que eso se hará automáticamente.

Si pretendes definir más de una sección de host, por ejemplo, con conexiones dependientes como un host de salto de proxy que depende de otro host de configuración, también puedes definir varias entradas de host aquí. XPipe lanzará entonces la primera entrada de host.

No tienes que realizar ningún formateo con espacios en blanco o sangrías, esto no es necesario para que funcione.

Ten en cuenta que debes tener cuidado de entrecomillar los valores si contienen espacios, de lo contrario se pasarán incorrectamente.

### Archivos de identidad

Ten en cuenta que aquí también puedes especificar una opción `Archivo de identidad`.
Si se especifica esta opción aquí, se ignorará cualquier otra opción de autenticación basada en claves que se especifique más adelante.

Si decides referirte a un archivo de identidad gestionado en la bóveda git de XPipe, también puedes hacerlo.
XPipe detectará los archivos de identidad compartidos y adaptará automáticamente la ruta del archivo en cada sistema en el que hayas clonado la bóveda git.
