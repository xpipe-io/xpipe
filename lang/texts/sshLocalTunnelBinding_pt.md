## Vinculação

A informação de ligação que forneces é passada diretamente para o cliente `ssh` da seguinte forma: `-L [origin_address:]origin_port:remote_address:remote_port`.

Por padrão, a origem será vinculada à interface de loopback se não for especificado de outra forma. Também podes utilizar quaisquer wildcards de endereço, e.g. definir o endereço para `0.0.0.0` de modo a ligar-se a todas as interfaces de rede acessíveis via IPv4. Quando omites completamente o endereço, será utilizado o wildcard `*`, que permite ligações em todas as interfaces de rede. Nota que algumas notações de interfaces de rede podem não ser suportadas em todos os sistemas operativos. Os servidores Windows, por exemplo, não suportam o curinga `*`.
