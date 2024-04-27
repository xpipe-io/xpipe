## X11 Forwarding

Lorsque cette option est activée, la connexion SSH sera lancée avec une redirection X11. Sous Linux, cela fonctionne généralement d'emblée et ne nécessite aucune configuration. Sur macOS, tu as besoin d'un serveur X11 comme [XQuartz] (https://www.xquartz.org/) sur ta machine locale.

### X11 sur Windows

XPipe te permet d'utiliser les capacités X11 de WSL2 pour ta connexion SSH. La seule chose dont tu as besoin pour cela est une distribution [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) installée sur ton système local. XPipe choisira automatiquement une distribution installée compatible si possible, mais tu peux aussi en utiliser une autre dans le menu des paramètres.

Cela signifie que tu n'as pas besoin d'installer un serveur X11 séparé sur Windows. Cependant, si tu en utilises un de toute façon, XPipe le détectera et utilisera le serveur X11 en cours d'exécution.

### Connexions X11 en tant que bureaux

Toute connexion SSH pour laquelle la redirection X11 est activée peut être utilisée comme hôte de bureau. Cela signifie que tu peux lancer des applications et des environnements de bureau par le biais de cette connexion. Lorsqu'une application de bureau est lancée, cette connexion sera automatiquement démarrée en arrière-plan pour lancer le tunnel X11.
