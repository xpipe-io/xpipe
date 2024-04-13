## Types d'exécution

Il existe deux types d'exécution distincts lorsque XPipe se connecte à un système.

### En arrière-plan

La première connexion à un système est effectuée en arrière-plan dans une session de terminal muet.

Les commandes de blocage qui nécessitent une entrée de la part de l'utilisateur peuvent geler le processus de l'interpréteur de commandes lorsque XPipe le démarre d'abord en interne en arrière-plan. Pour éviter cela, tu ne dois appeler ces commandes bloquantes qu'en mode terminal.

Le navigateur de fichiers, par exemple, utilise entièrement le mode d'arrière-plan muet pour gérer ses opérations, donc si tu veux que l'environnement de ton script s'applique à la session du navigateur de fichiers, il doit s'exécuter en mode muet.

### Dans les terminaux

Une fois que la connexion initiale au terminal muet a réussi, XPipe ouvre une connexion séparée dans le terminal réel. Si tu veux que le script soit exécuté lorsque tu ouvres la connexion dans un terminal, choisis le mode terminal.
