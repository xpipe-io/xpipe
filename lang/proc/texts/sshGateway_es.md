## Pasarelas de conexión Shell

Si está activado, XPipe abre primero una conexión shell con la pasarela y desde ahí abre una conexión SSH con el host especificado. El comando `ssh` debe estar disponible y localizado en el `PATH` de la pasarela elegida.

### Saltar servidores

Este mecanismo es similar a los servidores de salto, pero no equivalente. Es completamente independiente del protocolo SSH, por lo que puedes utilizar cualquier conexión shell como pasarela.

Si buscas servidores de salto SSH propiamente dichos, quizá también en combinación con el reenvío de agentes, utiliza la funcionalidad de conexión SSH personalizada con la opción de configuración `ProxyJump`.