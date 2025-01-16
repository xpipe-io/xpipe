## Vinculação

A informação de ligação que forneces é passada diretamente para o cliente `ssh` da seguinte forma: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Por padrão, o endereço de origem remota será vinculado à interface de loopback. Também podes utilizar quaisquer wildcards de endereço, por exemplo, definir o endereço para `0.0.0.0` de modo a ligar-se a todas as interfaces de rede acessíveis via IPv4. Quando omites completamente o endereço, será utilizado o wildcard `*`, que permite ligações em todas as interfaces de rede. Nota que algumas notações de interfaces de rede podem não ser suportadas em todos os sistemas operativos. Os servidores Windows, por exemplo, não suportam o curinga `*`.
