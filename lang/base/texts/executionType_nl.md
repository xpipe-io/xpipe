## Uitvoeringstypes

Er zijn twee verschillende uitvoeringstypen wanneer XPipe verbinding maakt met een systeem.

### Op de achtergrond

De eerste verbinding met een systeem wordt op de achtergrond gemaakt in een domme terminal sessie.

Blokkerende commando's die gebruikersinvoer vereisen kunnen het shell proces bevriezen wanneer XPipe het eerst intern op de achtergrond opstart. Om dit te voorkomen, moet je deze blokkerende commando's alleen in de terminalmodus aanroepen.

De bestandsbrowser bijvoorbeeld gebruikt volledig de domme achtergrondmodus om zijn bewerkingen af te handelen, dus als je wilt dat je scriptomgeving van toepassing is op de bestandsbrowsersessie, moet deze in de domme modus draaien.

### In de terminals

Nadat de initiële domme terminalverbinding is gelukt, opent XPipe een aparte verbinding in de echte terminal. Als je wilt dat het script wordt uitgevoerd wanneer je de verbinding in een terminal opent, kies dan de terminalmodus.
