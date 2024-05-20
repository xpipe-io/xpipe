### Ninguno

Desactiva la autenticación de `clave pública`.

### SSH-Agente

En caso de que tus identidades estén almacenadas en el SSH-Agent, el ejecutable ssh puede utilizarlas si se inicia el agente.
XPipe iniciará automáticamente el proceso del agente si aún no se está ejecutando.

### Pageant (Windows)

En caso de que estés utilizando pageant en Windows, XPipe comprobará primero si pageant se está ejecutando.
Debido a la naturaleza de pageant, es tu responsabilidad tenerlo
ya que tienes que especificar manualmente todas las claves que quieras añadir cada vez.
Si se está ejecutando, XPipe pasará la tubería con el nombre adecuado a través de
`-oIdentityAgent=...` a ssh, no tienes que incluir ningún archivo de configuración personalizado.

Ten en cuenta que hay algunos errores de implementación en el cliente OpenSSH que pueden causar problemas
si tu nombre de usuario contiene espacios o es demasiado largo, así que intenta utilizar la última versión.

### Pageant (Linux y macOS)

En caso de que tus identidades estén almacenadas en el agente pageant, el ejecutable ssh puede utilizarlas si se inicia el agente.
XPipe iniciará automáticamente el proceso del agente si aún no se está ejecutando.

### Archivo de identidad

También puedes especificar un archivo de identidad con una frase de contraseña opcional.
Esta opción es equivalente a `ssh -i <archivo>`.

Ten en cuenta que ésta debe ser la clave *privada*, no la pública.
Si te confundes, ssh sólo te dará crípticos mensajes de error.

### Agente GPG

Si tus identidades están almacenadas, por ejemplo, en una tarjeta inteligente, puedes optar por proporcionárselas al cliente SSH a través del `agente GPG`.
Esta opción habilitará automáticamente el soporte SSH del agente si aún no está habilitado y reiniciará el demonio del agente GPG con la configuración correcta.

### Yubikey PIV

Si tus identidades están almacenadas con la función de tarjeta inteligente PIV del Yubikey, puedes recuperarlas
con la biblioteca YKCS11 de Yubico, que viene incluida con Yubico PIV Tool.

Ten en cuenta que necesitas una versión actualizada de OpenSSH para utilizar esta función.

### Agente personalizado

También puedes utilizar un agente personalizado proporcionando aquí la ubicación del socket o la ubicación de la tubería con nombre.
Esto lo pasará a través de la opción `IdentityAgent`.

### Biblioteca PKCS#11 personalizada

Esto indicará al cliente OpenSSH que cargue el archivo de biblioteca compartida especificado, que se encargará de la autenticación.

Ten en cuenta que necesitas una versión actualizada de OpenSSH para utilizar esta función.
