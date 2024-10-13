# Identités VM SSH

Si l'utilisateur invité de la VM a besoin d'une authentification par clé pour SSH, tu peux l'activer ici.

Note qu'il est supposé que ta VM n'est pas exposée au public, et que le système hôte de la VM est donc utilisé comme passerelle SSH.
Par conséquent, toute option d'identité est spécifiée par rapport au système hôte de la VM et non par rapport à ta machine locale.
Toute clé spécifiée ici est interprétée comme un fichier sur l'hôte de la VM.
Si tu utilises un agent, celui-ci doit être exécuté sur le système hôte de la VM et non sur ta machine locale.

### Aucun

Si cette option est sélectionnée, XPipe ne fournira aucune identité. Cela désactive également toutes les sources externes telles que les agents.

### Fichier d'identité

Tu peux également spécifier un fichier d'identité avec une phrase de passe facultative.
Cette option est l'équivalent de `ssh -i <file>`.

Note qu'il doit s'agir de la clé *privée*, et non de la clé publique.
Si tu confonds les deux, ssh ne te donnera que des messages d'erreur énigmatiques.

### SSH-Agent

Si tes identités sont stockées dans l'agent SSH, l'exécutable ssh peut les utiliser si l'agent est démarré.
XPipe démarrera automatiquement le processus de l'agent s'il n'est pas encore en cours d'exécution.

Si l'agent n'est pas installé sur le système hôte de la VM, il est recommandé d'activer le transfert de l'agent SSH pour la connexion SSH d'origine à l'hôte de la VM.
Tu peux le faire en créant une connexion SSH personnalisée avec l'option `ForwardAgent` activée.

### Agent GPG

Si tes identités sont stockées par exemple sur une carte à puce, tu peux choisir de les fournir au client SSH via l'option `gpg-agent`.
Cette option activera automatiquement la prise en charge SSH de l'agent si elle n'est pas encore activée et redémarrera le démon de l'agent GPG avec les bons paramètres.

### Yubikey PIV

Si tes identités sont stockées avec la fonction de carte à puce PIV du Yubikey, tu peux les récupérer à l'aide du logiciel Yubico
les récupérer avec la bibliothèque YKCS11 de Yubico, qui est fournie avec Yubico PIV Tool.

Note que tu as besoin d'une version à jour d'OpenSSH pour utiliser cette fonction.

### Bibliothèque PKCS#11 personnalisée

Ceci demandera au client OpenSSH de charger le fichier de bibliothèque partagée spécifié, qui gérera l'authentification.

Note que tu as besoin d'une version à jour d'OpenSSH pour utiliser cette fonction.

### Pageant (Windows)

Si tu utilises pageant sous Windows, XPipe vérifiera d'abord si pageant est en cours d'exécution.
En raison de la nature de pageant, il est de ta responsabilité de le faire fonctionner
tu dois en effet spécifier manuellement toutes les clés que tu souhaites ajouter à chaque fois.
Si c'est le cas, XPipe passera le bon tuyau nommé via
`-oIdentityAgent=...` à ssh, tu n'as pas besoin d'inclure de fichiers de configuration personnalisés.

### Pageant (Linux & macOS)

Dans le cas où tes identités sont stockées dans l'agent pageant, l'exécutable ssh peut les utiliser si l'agent est démarré.
XPipe démarrera automatiquement le processus de l'agent s'il n'est pas encore en cours d'exécution.

### Autre source externe

Cette option permet à tout fournisseur d'identité externe en cours d'exécution de fournir ses clés au client SSH. Tu devrais utiliser cette option si tu utilises un autre agent ou un gestionnaire de mots de passe pour gérer tes clés SSH.
