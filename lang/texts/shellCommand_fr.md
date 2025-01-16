## Connexions shell personnalisées

Ouvre un shell à l'aide de la commande personnalisée en exécutant la commande donnée sur le système hôte sélectionné. Ce shell peut être local ou distant.

Note que cette fonctionnalité s'attend à ce que le shell soit d'un type standard tel que `cmd`, `bash`, etc. Si tu veux ouvrir d'autres types de shells et de commandes dans un terminal, tu peux utiliser le type de commande custom terminal à la place. L'utilisation de shells standard te permet également d'ouvrir cette connexion dans le navigateur de fichiers.

### Invites interactives

Le processus de l'interpréteur de commandes peut s'interrompre ou se bloquer en cas d'invite de saisie inattendue, comme une invite de mot de passe
inattendue, comme une invite de mot de passe. Par conséquent, tu dois toujours t'assurer qu'il n'y a pas d'invite de saisie interactive.

Par exemple, une commande comme `ssh user@host` fonctionnera bien ici tant qu'il n'y a pas de mot de passe requis.

### Les interpréteurs de commandes locaux personnalisés

Dans de nombreux cas, il est utile de lancer un shell avec certaines options qui sont généralement désactivées par défaut afin de faire fonctionner correctement certains scripts et certaines commandes. Par exemple :

-   [Expansion retardée dans
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Exécution Powershell
    powershell](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Mode POSIX de Bash
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Et toute autre option de lancement possible pour un shell de ton choix

Cela peut être réalisé en créant des commandes shell personnalisées avec par exemple les commandes suivantes :

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`