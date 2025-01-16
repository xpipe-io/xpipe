## Script d'initialisation

Les commandes facultatives à exécuter après que les fichiers et profils init de l'interpréteur de commandes ont été exécutés.

Tu peux traiter ce script comme un script shell normal, c'est-à-dire utiliser toute la syntaxe que le shell prend en charge dans les scripts. Toutes les commandes que tu exécutes sont générées par l'interpréteur de commandes et modifient l'environnement. Ainsi, si tu définis par exemple une variable, tu auras accès à cette variable dans cette session de l'interpréteur de commandes.

### Commandes bloquantes

Note que les commandes de blocage qui nécessitent une entrée de la part de l'utilisateur peuvent geler le processus de l'interpréteur de commandes lorsque XPipe le démarre d'abord en interne en arrière-plan. Pour éviter cela, n'appelle ces commandes bloquantes que si la variable `TERM` n'est pas définie sur `dumb`. XPipe définit automatiquement la variable `TERM=dumb` lorsqu'il prépare la session shell en arrière-plan, puis définit `TERM=xterm-256color` lors de l'ouverture effective du terminal.