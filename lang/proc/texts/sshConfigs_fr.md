### Configurations SSH

XPipe charge tous les hôtes et applique tous les paramètres que tu as configurés dans le fichier sélectionné. Ainsi, en spécifiant une option de configuration globale ou spécifique à un hôte, elle sera automatiquement appliquée à la connexion établie par XPipe.

Si tu veux en savoir plus sur l'utilisation des configs SSH, tu peux utiliser `man ssh_config` ou lire ce [guide](https://www.ssh.com/academy/ssh/config).

### Identités

Note que tu peux également spécifier une option `IdentityFile` ici. Si une identité est spécifiée ici, toute autre identité spécifiée plus bas sera ignorée.

### Redirection X11

Si des options pour la redirection X11 sont spécifiées ici, XPipe tentera automatiquement de mettre en place la redirection X11 sur Windows par l'intermédiaire de WSL.