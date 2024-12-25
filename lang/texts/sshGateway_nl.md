## Shell verbindingspoorten

Indien ingeschakeld, opent XPipe eerst een shell verbinding naar de gateway en van daaruit een SSH verbinding naar de opgegeven host. Het `ssh` commando moet beschikbaar zijn en zich in het `PATH` op de gekozen gateway bevinden.

### Servers springen

Dit mechanisme lijkt op jump servers, maar is niet gelijkwaardig. Het is volledig onafhankelijk van het SSH protocol, dus je kunt iedere shell verbinding als gateway gebruiken.

Als je op zoek bent naar goede SSH jump servers, misschien ook in combinatie met agent forwarding, gebruik dan de aangepaste SSH verbindingsfunctionaliteit met de `ProxyJump` configuratie optie.