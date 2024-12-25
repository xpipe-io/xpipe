## Interaction avec le système

XPipe essaie de détecter le type de shell dans lequel il s'est connecté pour vérifier que tout a fonctionné correctement et pour afficher des informations sur le système. Cela fonctionne pour les shells de commande normaux comme bash, mais échoue pour les shells de connexion non standard et personnalisés de nombreux systèmes embarqués. Tu dois désactiver ce comportement pour que les connexions à ces systèmes réussissent.

Lorsque cette interaction est désactivée, elle n'essaiera pas d'identifier les informations du système. Cela empêchera le système d'être utilisé dans le navigateur de fichiers ou comme système proxy/passerelle pour d'autres connexions. XPipe agira alors essentiellement comme un lanceur de connexion.
