## Interacción con el sistema

XPipe intenta detectar en qué shell se ha iniciado sesión para verificar que todo funciona correctamente y para mostrar información del sistema. Esto funciona para los shells de comandos normales como bash, pero falla para los shells de inicio de sesión no estándar y personalizados de muchos sistemas embebidos. Tienes que desactivar este comportamiento para que las conexiones a estos sistemas tengan éxito.

Cuando esta interacción está desactivada, no intentará identificar ninguna información del sistema. Esto impedirá que el sistema se utilice en el explorador de archivos o como sistema proxy/pasarela para otras conexiones. XPipe se limitará entonces a actuar como lanzador de la conexión.
