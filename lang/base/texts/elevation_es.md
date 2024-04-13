## Elevación

El proceso de elevación es específico del sistema operativo.

### Linux y macOS

Cualquier comando elevado se ejecuta con `sudo`. La contraseña opcional `sudo` se consulta a través de XPipe cuando es necesario.
Tienes la posibilidad de ajustar el comportamiento de elevación en la configuración para controlar si quieres introducir tu contraseña cada vez que se necesite o si quieres guardarla en caché para la sesión actual.

### Windows

En Windows, no es posible elevar un proceso hijo si el proceso padre no está también elevado.
Por lo tanto, si XPipe no se ejecuta como administrador, no podrás utilizar ninguna elevación localmente.
Para las conexiones remotas, la cuenta de usuario conectada debe tener privilegios de administrador.