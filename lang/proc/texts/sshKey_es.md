### Ninguno

Si se selecciona, XPipe no proporcionará ninguna identidad. Esto también desactiva cualquier fuente externa como los agentes.

### Archivo de identidad

También puedes especificar un archivo de identidad con una frase de contraseña opcional.
Esta opción equivale a `ssh -i <archivo>`.

Ten en cuenta que ésta debe ser la clave *privada*, no la pública.
Si te confundes, ssh sólo te dará crípticos mensajes de error.

### Agente SSH

En caso de que tus identidades estén almacenadas en el SSH-Agent, el ejecutable ssh podrá utilizarlas si se inicia el agente.
XPipe iniciará automáticamente el proceso del agente si aún no se está ejecutando.

### Agente GPG

Si tus identidades están almacenadas, por ejemplo, en una tarjeta inteligente, puedes elegir proporcionarlas al cliente SSH a través del `agente GPG`.
Esta opción habilitará automáticamente el soporte SSH del agente si aún no está habilitado y reiniciará el demonio del agente GPG con la configuración correcta.

### Yubikey PIV

Si tus identidades están almacenadas con la función de tarjeta inteligente PIV del Yubikey, puedes recuperarlas
con la biblioteca YKCS11 de Yubico, que viene incluida con Yubico PIV Tool.

Ten en cuenta que necesitas una versión actualizada de OpenSSH para utilizar esta función.

### Biblioteca PKCS#11 personalizada

Esto indicará al cliente OpenSSH que cargue el archivo de biblioteca compartida especificado, que se encargará de la autenticación.

Ten en cuenta que necesitas una versión actualizada de OpenSSH para utilizar esta función.

### Pageant (Windows)

Si utilizas pageant en Windows, XPipe comprobará primero si pageant se está ejecutando.
Debido a la naturaleza de pageant, es tu responsabilidad tenerlo
ya que tienes que especificar manualmente todas las claves que quieras añadir cada vez.
Si se está ejecutando, XPipe pasará la tubería con el nombre adecuado a través de
`-oIdentityAgent=...` a ssh, no tienes que incluir ningún archivo de configuración personalizado.

### Pageant (Linux y macOS)

En caso de que tus identidades estén almacenadas en el agente pageant, el ejecutable ssh puede utilizarlas si se inicia el agente.
XPipe iniciará automáticamente el proceso del agente si aún no se está ejecutando.

### Otra fuente externa

Esta opción permitirá que cualquier proveedor de identidad externo en ejecución suministre sus claves al cliente SSH. Debes utilizar esta opción si utilizas cualquier otro agente o gestor de contraseñas para gestionar tus claves SSH.
