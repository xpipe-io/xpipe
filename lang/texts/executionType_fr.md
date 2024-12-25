# Types d'exécution

Tu peux utiliser un script dans plusieurs scénarios différents.

Lors de l'activation d'un script via son bouton bascule d'activation, les types d'exécution dictent ce que XPipe fera avec le script.

## Type de script d'initialisation

Lorsqu'un script est désigné comme script init, il peut être sélectionné dans les environnements shell pour être exécuté lors de l'init.

De plus, si un script est activé, il sera automatiquement exécuté lors de l'initialisation dans tous les shells compatibles.

Par exemple, si tu crées un script init simple avec
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
tu auras accès à ces alias dans toutes les sessions shell compatibles si le script est activé.

## Type de script exécutable

Un script shell exécutable est destiné à être appelé pour une certaine connexion à partir du hub de connexion.
Lorsque ce script est activé, il sera possible de l'appeler à partir du bouton scripts pour une connexion avec un dialecte shell compatible.

Par exemple, si tu crées un simple script shell de dialecte `sh` nommé `ps` pour afficher la liste des processus en cours avec
```
ps -A
```
tu peux appeler le script sur n'importe quelle connexion compatible dans le menu des scripts.

## Fichier type de script

Enfin, tu peux aussi exécuter des scripts personnalisés avec des entrées de fichiers à partir de l'interface du navigateur de fichiers.
Lorsqu'un script de fichier est activé, il s'affiche dans le navigateur de fichiers pour être exécuté avec des entrées de fichier.

Par exemple, si tu crées un script de fichier simple avec
```
diff "$1" "$2"
```
tu peux exécuter le script sur les fichiers sélectionnés si le script est activé.
Dans cet exemple, le script ne s'exécutera avec succès que si tu as exactement deux fichiers sélectionnés.
Sinon, la commande diff échouera.

## Session shell type de script

Un script de session est destiné à être appelé dans une session shell dans ton terminal.
Lorsqu'il est activé, le script est copié sur le système cible et placé dans le chemin d'accès (PATH) de tous les shells compatibles.
Cela te permet d'appeler le script depuis n'importe quel endroit d'une session de terminal.
Le nom du script sera en minuscules et les espaces seront remplacés par des traits de soulignement, ce qui te permettra d'appeler facilement le script.

Par exemple, si tu crées un script shell simple pour les dialectes `sh` nommé `apti` avec
```
sudo apt install "$1"
```
tu peux appeler le script sur n'importe quel système compatible avec `apti.sh <pkg>` dans une session de terminal si le script est activé.

## Plusieurs types

Tu peux aussi cocher plusieurs cases pour les types d'exécution d'un script s'ils doivent être utilisés dans plusieurs scénarios.
