# Udførelsestyper

Du kan bruge et script i flere forskellige scenarier.

Når du aktiverer et script via dets aktiveringsknap, dikterer udførelsestyperne, hvad XPipe vil gøre med scriptet.

## Init-script-type

Når et script er angivet som init-script, kan det vælges i shell-miljøer til at blive kørt ved init.

Hvis et script er aktiveret, vil det desuden automatisk blive kørt ved init i alle kompatible shells.

Hvis du f.eks. opretter et simpelt init-script med
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
du vil have adgang til disse aliasser i alle kompatible shell-sessioner, hvis scriptet er aktiveret.

## Kørbar script-type

Et kørbart shell-script er beregnet til at blive kaldt for en bestemt forbindelse fra forbindelseshubben.
Når dette script er aktiveret, vil scriptet være tilgængeligt for kald fra scripts-knappen for en forbindelse med en kompatibel shell-dialekt.

Hvis du f.eks. opretter et simpelt `sh`-dialekt-shellscript med navnet `ps` for at vise den aktuelle procesliste med
```
ps -A
```
kan du kalde scriptet på enhver kompatibel forbindelse i menuen scripts.

## Fil script type

Endelig kan du også køre brugerdefinerede scripts med filinput fra filbrowserens grænseflade.
Når et filscript er aktiveret, vises det i filbrowseren, så det kan køres med filinput.

Hvis du f.eks. opretter et simpelt filscript med
```
diff "$1" "$2"
```
kan du køre scriptet på udvalgte filer, hvis scriptet er aktiveret.
I dette eksempel vil scriptet kun køre, hvis du har valgt præcis to filer.
Ellers vil diff-kommandoen mislykkes.

## Shell-session script type

Et sessionsscript er beregnet til at blive kaldt i en shell-session i din terminal.
Når det er aktiveret, vil scriptet blive kopieret til målsystemet og lagt i PATH i alle kompatible shells.
På den måde kan du kalde scriptet hvor som helst i en terminalsession.
Scriptnavnet skrives med små bogstaver, og mellemrum erstattes med understregninger, så du nemt kan kalde scriptet.

Hvis du for eksempel opretter et simpelt shellscript til `sh`-dialekter ved navn `apti` med
```
sudo apt install "$1"
```
kan du kalde scriptet på ethvert kompatibelt system med `apti.sh <pkg>` i en terminalsession, hvis scriptet er aktiveret.

## Flere typer

Du kan også markere flere felter for udførelsestyper af et script, hvis de skal bruges i flere scenarier.
