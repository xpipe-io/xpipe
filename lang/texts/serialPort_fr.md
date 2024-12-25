## Windows

Sur les systèmes Windows, tu fais généralement référence aux ports série via `COM<index>`.
XPipe prend également en charge la spécification de l'index sans le préfixe `COM`.
Pour adresser des ports supérieurs à 9, il faut utiliser la forme de chemin UNC avec `\\N-COM<index>`.

Si tu as installé une distribution WSL1, tu peux aussi référencer les ports série à partir de la distribution WSL via `/dev/ttyS<index>`.
Cela ne fonctionne plus avec WSL2.
Si tu as un système WSL1, tu peux utiliser celui-ci comme hôte pour cette connexion série et utiliser la notation tty pour y accéder avec XPipe.

## Linux

Sur les systèmes Linux, tu peux généralement accéder aux ports série via `/dev/ttyS<index>`.
Si tu connais l'ID de l'appareil connecté mais que tu ne veux pas garder trace du port série, tu peux aussi les référencer via `/dev/serial/by-id/<device id>`.
Tu peux dresser la liste de tous les ports série disponibles avec leur ID en exécutant `ls /dev/serial/by-id/*`.

## macOS

Sur macOS, les noms des ports série peuvent être à peu près n'importe quoi, mais ils ont généralement la forme `/dev/tty.<id>` où l'id l'identifiant interne du périphérique.
L'exécution de `ls /dev/tty.*` devrait permettre de trouver les ports série disponibles.
