## Détection du type de coquille

XPipe fonctionne en détectant le type de shell de la connexion et en interagissant ensuite avec le shell actif. Cette approche ne fonctionne cependant que lorsque le type de shell est connu et qu'il prend en charge un certain nombre d'actions et de commandes. Tous les shells courants comme `bash`, `cmd`, `powershell`, et bien d'autres, sont pris en charge.

## Types d'interprètes de commandes inconnus

Si tu te connectes à un système qui n'exécute pas un shell de commande connu, par exemple un routeur, un lien ou un appareil IOT, XPipe sera incapable de détecter le type de shell et se trompera au bout d'un certain temps. En activant cette option, XPipe n'essaiera pas d'identifier le type de shell et lancera le shell tel quel. Cela te permet d'ouvrir la connexion sans erreur, mais de nombreuses fonctionnalités, par exemple le navigateur de fichiers, les scripts, les sous-connexions, et plus encore, ne seront pas prises en charge pour cette connexion.
