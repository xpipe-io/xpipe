### Aucun

Désactive l'authentification par `publickey`.

### SSH-Agent

Dans le cas où tes identités sont stockées dans le SSH-Agent, l'exécutable ssh peut les utiliser si l'agent est démarré.
XPipe démarrera automatiquement le processus de l'agent s'il n'est pas encore en cours d'exécution.

### Pageant (Windows)

Si tu utilises pageant sous Windows, XPipe vérifiera d'abord si pageant est en cours d'exécution.
En raison de la nature de pageant, il est de ta responsabilité de le faire fonctionner
tu dois en effet spécifier manuellement toutes les clés que tu souhaites ajouter à chaque fois.
S'il fonctionne, XPipe passera le bon tuyau nommé via
`-oIdentityAgent=...` à ssh, tu n'as pas besoin d'inclure de fichiers de configuration personnalisés.

Note qu'il y a quelques bogues d'implémentation dans le client OpenSSH qui peuvent causer des problèmes
si ton nom d'utilisateur contient des espaces ou est trop long, alors essaie d'utiliser la dernière version.

### Pageant (Linux & macOS)

Dans le cas où tes identités sont stockées dans l'agent pageant, l'exécutable ssh peut les utiliser si l'agent est démarré.
XPipe démarrera automatiquement le processus de l'agent s'il n'est pas encore en cours d'exécution.

### Fichier d'identité

Tu peux également spécifier un fichier d'identité avec une phrase de passe optionnelle.
Cette option est l'équivalent de `ssh -i <fichier>`.

Note qu'il doit s'agir de la clé *privée*, et non de la clé publique.
Si tu confonds les deux, ssh ne te donnera que des messages d'erreur énigmatiques.

### Agent GPG

Si tes identités sont stockées par exemple sur une carte à puce, tu peux choisir de les fournir au client SSH via le `gpg-agent`.
Cette option activera automatiquement la prise en charge SSH de l'agent si elle n'est pas encore activée et redémarrera le démon de l'agent GPG avec les bons paramètres.

### Yubikey PIV

Si tes identités sont stockées avec la fonction de carte à puce PIV du Yubikey, tu peux les récupérer à l'aide du logiciel Yubico
les récupérer avec la bibliothèque YKCS11 de Yubico, qui est fournie avec Yubico PIV Tool.

Note que tu as besoin d'une version à jour d'OpenSSH pour utiliser cette fonction.

### Agent personnalisé

Tu peux également utiliser un agent personnalisé en indiquant ici l'emplacement de la prise ou l'emplacement du tuyau nommé.
Ceci sera transmis via l'option `IdentityAgent`.

### Bibliothèque PKCS#11 personnalisée

Ceci demandera au client OpenSSH de charger le fichier de bibliothèque partagée spécifié, qui gérera l'authentification.

Note que tu as besoin d'une version à jour d'OpenSSH pour utiliser cette fonction.
