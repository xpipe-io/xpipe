## Élévation

Le processus d'élévation des permissions est spécifique au système d'exploitation.

### Linux et macOS

Toute commande élevée est exécutée avec `sudo`. Le mot de passe facultatif `sudo` est interrogé via XPipe lorsque cela est nécessaire. Tu as la possibilité d'ajuster le comportement d'élévation dans les paramètres pour contrôler si tu veux saisir ton mot de passe à chaque fois qu'il est nécessaire ou si tu veux le mettre en cache pour la session en cours.

### Windows

Sous Windows, il n'est pas possible d'élever les permissions d'un processus enfant si le processus parent ne s'exécute pas également avec des permissions élevées. Par conséquent, si XPipe n'est pas exécuté en tant qu'administrateur, tu ne pourras pas utiliser d'élévation localement. Pour les connexions à distance, le compte d'utilisateur connecté doit avoir des privilèges d'administrateur.