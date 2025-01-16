## Dépendances du script

Les scripts et les groupes de scripts à exécuter en premier. Si un groupe entier devient une dépendance, tous les scripts de ce groupe seront considérés comme des dépendances.

Le graphe de dépendance résolu des scripts est aplati, filtré et rendu unique. C'est-à-dire que seuls les scripts compatibles seront exécutés et que si un script devait être exécuté plusieurs fois, il ne sera exécuté que la première fois.
