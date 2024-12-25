## Système cible VNC

En plus des fonctions VNC normales, XPipe ajoute également des fonctions supplémentaires en interagissant avec le shell du système cible.

Dans certains cas, l'hôte du serveur VNC, c'est-à-dire le système distant sur lequel tourne le serveur VNC, peut être différent du système réel que tu contrôles avec VNC. Par exemple, si un serveur VNC est géré par un hyperviseur VM comme Proxmox, le serveur s'exécute sur l'hôte de l'hyperviseur alors que le système cible réel que tu contrôles, par exemple une VM, est l'invité de la VM. Pour t'assurer que les opérations sur le système de fichiers, par exemple, sont appliquées sur le bon système, tu peux modifier manuellement le système cible s'il diffère de l'hôte du serveur VNC.