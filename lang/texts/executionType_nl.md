# Uitvoeringstypen

Je kunt een script in meerdere verschillende scenario's gebruiken.

Wanneer je een script inschakelt via de inschakelknop, bepalen de uitvoeringstypen wat XPipe met het script zal doen.

## Init script type

Als een script is aangewezen als init-script, kan het in shell-omgevingen worden geselecteerd om bij init te worden uitgevoerd.

Bovendien, als een script is ingeschakeld, zal het automatisch op init worden uitgevoerd in alle compatibele shells.

Als je bijvoorbeeld een eenvoudig init-script maakt met
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
je hebt toegang tot deze aliassen in alle compatibele shell sessies als het script is ingeschakeld.

## Runnable scripttype

Een uitvoerbaar shellscript is bedoeld om te worden aangeroepen voor een bepaalde verbinding vanuit de verbindingshub.
Als dit script is ingeschakeld, is het script beschikbaar om aan te roepen via de scripts knop voor een verbinding met een compatibel shell dialect.

Als je bijvoorbeeld een eenvoudig `sh` dialect shellscript maakt met de naam `ps` om de huidige proceslijst te tonen met
```
ps -A
```
kun je het script op elke compatibele verbinding aanroepen in het menu scripts.

## Type bestandsscript

Tot slot kun je ook aangepaste scripts uitvoeren met bestandsinvoer vanuit de bestandsbrowserinterface.
Als een bestandsscript is ingeschakeld, verschijnt het in de bestandsbrowser om te worden uitgevoerd met bestandsinvoer.

Als je bijvoorbeeld een eenvoudig bestandsscript maakt met
```
diff "$1" "$2"
```
kun je het script uitvoeren op geselecteerde bestanden als het script is ingeschakeld.
In dit voorbeeld zal het script alleen succesvol draaien als je precies twee bestanden hebt geselecteerd.
Anders zal het diff commando mislukken.

## Shell sessie script type

Een sessie script is bedoeld om aangeroepen te worden in een shell sessie in je terminal.
Als het is ingeschakeld, wordt het script gekopieerd naar het doelsysteem en in het PATH van alle compatibele shells gezet.
Hierdoor kun je het script overal vandaan in een terminalsessie aanroepen.
De scriptnaam wordt met kleine letters geschreven en spaties worden vervangen door underscores, zodat je het script gemakkelijk kunt aanroepen.

Als je bijvoorbeeld een eenvoudig shellscript maakt voor `sh` dialecten met de naam `apti` met
```
sudo apt install "$1"
```
kun je het script op elk compatibel systeem aanroepen met `apti.sh <pkg>` in een terminal sessie als het script is ingeschakeld.

## Meerdere types

Je kunt ook meerdere vakjes aanvinken voor uitvoeringstypen van een script als ze in meerdere scenario's moeten worden gebruikt.
