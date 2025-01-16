## Init script

De optionele commando's om uit te voeren nadat de init bestanden en profielen van de shell zijn uitgevoerd.

Je kunt dit behandelen als een normaal shellscript, dus gebruik maken van alle syntaxis die de shell ondersteunt in scripts. Alle commando's die je uitvoert zijn afkomstig van de shell en wijzigen de omgeving. Dus als je bijvoorbeeld een variabele instelt, heb je toegang tot deze variabele in deze shellsessie.

### Blokkerende commando's

Merk op dat blokkeringscommando's die gebruikersinvoer vereisen het shell proces kunnen bevriezen als XPipe het eerst intern op de achtergrond opstart. Om dit te voorkomen, roep je deze blokkerende commando's alleen aan als de variabele `TERM` niet is ingesteld op `dumb`. XPipe stelt automatisch de variabele `TERM=dumb` in wanneer het de shellsessie op de achtergrond voorbereidt en stelt vervolgens `TERM=xterm-256color` in wanneer het daadwerkelijk de terminal opent.