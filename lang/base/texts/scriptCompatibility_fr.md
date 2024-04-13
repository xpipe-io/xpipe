## Compatibilité des scripts

Le type de shell contrôle l'endroit où ce script peut être exécuté.
Outre une correspondance exacte, c'est-à-dire l'exécution d'un script `zsh` dans `zsh`, XPipe inclura également une vérification plus large de la compatibilité.

### Shells Posix

Tout script déclaré comme un script `sh` est capable de s'exécuter dans n'importe quel environnement shell lié à Posix, tel que `bash` ou `zsh`.
Si tu as l'intention d'exécuter un script de base sur de nombreux systèmes différents, utiliser uniquement des scripts de syntaxe `sh` est la meilleure solution pour cela.

### PowerShell

Les scripts déclarés comme des scripts `powershell` normaux sont également capables de s'exécuter dans des environnements `pwsh`.
