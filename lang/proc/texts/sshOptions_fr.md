## Configurations SSH

Ici, tu peux spécifier toutes les options SSH qui doivent être transmises à la connexion.
Alors que certaines options sont essentiellement nécessaires pour établir avec succès une connexion, comme `HostName`,
de nombreuses autres options sont purement facultatives.

Pour avoir une vue d'ensemble de toutes les options possibles, tu peux utiliser [`man ssh_config`](https://linux.die.net/man/5/ssh_config) ou lire ce [guide](https://www.ssh.com/academy/ssh/config).
Le nombre exact d'options prises en charge dépend purement du client SSH que tu as installé.

### Formatage

Le contenu ici est équivalent à une section d'hôte dans un fichier de configuration SSH.
Note que tu n'as pas besoin de définir explicitement la clé `Host`, car cela sera fait automatiquement.

Si tu as l'intention de définir plus d'une section d'hôte, par exemple avec des connexions dépendantes telles qu'un hôte de saut de proxy qui dépend d'un autre hôte de configuration, tu peux définir plusieurs entrées d'hôte ici aussi. XPipe lancera alors la première entrée d'hôte.

Tu n'as pas à effectuer de formatage avec des espaces blancs ou des indentations, ce n'est pas nécessaire pour que cela fonctionne.

Note que tu dois prendre soin de mettre les valeurs entre guillemets si elles contiennent des espaces, sinon elles seront transmises de manière incorrecte.

### Fiches d'identité

Note que tu peux également spécifier une option `IdentityFile` ici.
Si cette option est spécifiée ici, toute option d'authentification par clé spécifiée par ailleurs plus bas sera ignorée.

Si tu choisis de faire référence à un fichier d'identité géré dans le coffre-fort git de XPipe, tu peux également le faire.
XPipe détectera les fichiers d'identité partagés et adaptera automatiquement le chemin du fichier sur chaque système sur lequel tu as cloné le coffre-fort git.
