## Windows

Nos sistemas Windows, normalmente referes-te às portas série através de `COM<index>`.
O XPipe também suporta apenas a especificação do índice sem o prefixo `COM`.
Para endereçar portas maiores que 9, é necessário usar a forma de caminho UNC com `\\.\COM<index>`.

Se tiver uma distribuição WSL1 instalada, também pode referenciar as portas seriais de dentro da distribuição WSL via `/dev/ttyS<index>`.
No entanto, isso não funciona mais com o WSL2.
Se tiveres um sistema WSL1, podes usar este como anfitrião para esta ligação série e usar a notação tty para aceder a ele com o XPipe.

## Linux

Em sistemas Linux podes tipicamente aceder às portas série via `/dev/ttyS<index>`.
Se souberes o ID do dispositivo ligado mas não quiseres manter o registo da porta série, podes também referenciá-los através de `/dev/serial/by-id/<device id>`.
Podes listar todas as portas série disponíveis com os seus IDs ao correr `ls /dev/serial/by-id/*`.

## macOS

No macOS, os nomes das portas seriais podem ser praticamente qualquer coisa, mas geralmente têm a forma de `/dev/tty.<id>` onde o id é o identificador interno do dispositivo.
Executar `ls /dev/tty.*` deve encontrar portas seriais disponíveis.
