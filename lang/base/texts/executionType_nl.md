# Uitvoeringstypen

Je kunt een script in meerdere verschillende scenario's gebruiken.

Wanneer je een script inschakelt, bepalen de uitvoeringstypen wat XPipe met het script zal doen.

## Init scripts

Als een script is aangewezen als init-script, kan het worden geselecteerd in shell-omgevingen.

Bovendien, als een script is ingeschakeld, zal het automatisch worden uitgevoerd op init in alle compatibele shells.

Als je bijvoorbeeld een eenvoudig init-script maakt als
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
je hebt toegang tot deze aliassen in alle compatibele shell sessies als het script is ingeschakeld.

## Shell scripts

Een normaal shellscript is bedoeld om aangeroepen te worden in een shellsessie in je terminal.
Als dit is ingeschakeld, wordt het script gekopieerd naar het doelsysteem en in het PATH van alle compatibele shells gezet.
Hierdoor kun je het script overal vandaan in een terminalsessie aanroepen.
De scriptnaam wordt met kleine letters geschreven en spaties worden vervangen door underscores, zodat je het script gemakkelijk kunt aanroepen.

Als je bijvoorbeeld een eenvoudig shellscript maakt met de naam `apti` zoals
```
sudo apt install "$1"
```
kun je dat op elk compatibel systeem aanroepen met `apti.sh <pkg>` als het script is ingeschakeld.

## Bestandsscripts

Tot slot kun je ook aangepaste scripts uitvoeren met bestandsinvoer vanuit de bestandsbrowserinterface.
Als een bestandsscript is ingeschakeld, verschijnt het in de bestandsbrowser om te worden uitgevoerd met bestandsinvoer.

Als je bijvoorbeeld een eenvoudig bestandsscript maakt zoals
```
sudo apt install "$@"
```
kun je het script uitvoeren op geselecteerde bestanden als het script is ingeschakeld.

## Meerdere types

Aangezien het voorbeeldbestandsscript hetzelfde is als het voorbeeldshell-script hierboven,
zie je dat je ook meerdere vakjes kunt aanvinken voor uitvoeringstypen van een script als ze in meerdere scenario's moeten worden gebruikt.


