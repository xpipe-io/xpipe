## Vinculación

La información de enlace que proporciones se pasa directamente al cliente `ssh` de la siguiente forma: `-L [direccion_origen:]puerto_origen:direccion_remota:puerto_remoto`.

Por defecto, el origen se enlazará a la interfaz loopback si no se especifica lo contrario. También puedes utilizar cualquier comodín de dirección, por ejemplo, establecer la dirección en `0.0.0.0` para enlazar con todas las interfaces de red accesibles a través de IPv4. Si omites completamente la dirección, se utilizará el comodín `*`, que permite conexiones en todas las interfaces de red. Ten en cuenta que algunas notaciones de interfaces de red pueden no ser compatibles con todos los sistemas operativos. Los servidores Windows, por ejemplo, no admiten el comodín `*`.
