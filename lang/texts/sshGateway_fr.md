## Passerelles de connexion Shell

Si cette option est activée, XPipe ouvre d'abord une connexion shell à la passerelle et, à partir de là, une connexion SSH à l'hôte spécifié. La commande `ssh` doit être disponible et se trouver dans le `PATH` sur la passerelle que tu as choisie.

### Sauter les serveurs

Ce mécanisme est similaire aux serveurs de saut, mais il n'est pas équivalent. Il est complètement indépendant du protocole SSH, tu peux donc utiliser n'importe quelle connexion shell comme passerelle.

Si tu cherches des serveurs de saut SSH proprement dits, peut-être aussi en combinaison avec le transfert d'agent, utilise la fonctionnalité de connexion SSH personnalisée avec l'option de configuration `ProxyJump`.