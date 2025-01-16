## Vinculación

La información de enlace que proporciones se pasa directamente al cliente `ssh` de la siguiente forma: `-R [dirección_de_origen_remoto:]puerto_de_origen_remoto:dirección_de_destino_origen:puerto_de_destino_origen`.

Por defecto, la dirección de origen remota se enlazará a la interfaz loopback. También puedes utilizar cualquier comodín de dirección, por ejemplo, establecer la dirección en `0.0.0.0` para enlazar con todas las interfaces de red accesibles a través de IPv4. Si omites completamente la dirección, se utilizará el comodín `*`, que permite conexiones en todas las interfaces de red. Ten en cuenta que algunas notaciones de interfaces de red pueden no ser compatibles con todos los sistemas operativos. Los servidores Windows, por ejemplo, no admiten el comodín `*`.
