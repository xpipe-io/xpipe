# Options RDP supplémentaires

Si tu veux personnaliser davantage ta connexion, tu peux le faire en fournissant des propriétés RDP de la même manière qu'elles sont contenues dans les fichiers .rdp. Pour une liste complète des propriétés disponibles, voir la [documentation RDP] (https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Ces options ont le format `option:type:valeur`. Ainsi, par exemple, pour personnaliser la taille de la fenêtre du bureau, tu peux passer la configuration suivante :
```
desktopwidth:i:*width*
desktopopheight:i:*hauteur*
```
