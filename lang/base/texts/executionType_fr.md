# Types d'exécution

Tu peux utiliser un script dans plusieurs scénarios différents.

Lors de l'activation d'un script, les types d'exécution dictent ce que XPipe fera avec le script.

## Init scripts

Lorsqu'un script est désigné comme script init, il peut être sélectionné dans les environnements shell.

De plus, si un script est activé, il sera automatiquement exécuté lors de l'init dans tous les shells compatibles.

Par exemple, si tu crées un script init simple comme
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
tu auras accès à ces alias dans toutes les sessions shell compatibles si le script est activé.

## Scripts shell

Un script shell normal est destiné à être appelé dans une session shell dans ton terminal.
Lorsqu'il est activé, le script sera copié sur le système cible et placé dans le chemin d'accès (PATH) de tous les shells compatibles.
Cela te permet d'appeler le script depuis n'importe quel endroit d'une session de terminal.
Le nom du script sera en minuscules et les espaces seront remplacés par des traits de soulignement, ce qui te permettra d'appeler facilement le script.

Par exemple, si tu crées un script shell simple nommé `apti` comme suit 
```
sudo apt install "$1"
```
vous pouvez l'appeler sur n'importe quel système compatible avec `apti.sh <pkg>` si le script est activé.

## Fichier scripts

Enfin, tu peux aussi exécuter des scripts personnalisés avec des entrées de fichiers à partir de l'interface du navigateur de fichiers.
Lorsqu'un script de fichier est activé, il s'affiche dans le navigateur de fichiers pour être exécuté avec des entrées de fichier.

Par exemple, si tu crées un script de fichier simple comme
```
sudo apt install "$@"
```
tu peux exécuter le script sur les fichiers sélectionnés si le script est activé.

## Plusieurs types

Comme l'exemple de script de fichier est le même que l'exemple de script shell ci-dessus,
tu vois que tu peux aussi cocher plusieurs cases pour les types d'exécution d'un script s'ils doivent être utilisés dans plusieurs scénarios.


