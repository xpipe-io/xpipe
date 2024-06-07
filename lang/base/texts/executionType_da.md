# Udførelsestyper

Du kan bruge et script i flere forskellige scenarier.

Når du aktiverer et script, dikterer udførelsestyperne, hvad XPipe vil gøre med scriptet.

## Init-scripts

Når et script er angivet som init-script, kan det vælges i shell-miljøer.

Hvis et script er aktiveret, vil det desuden automatisk blive kørt ved init i alle kompatible shells.

Hvis du f.eks. opretter et simpelt init-script som
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
du vil have adgang til disse aliasser i alle kompatible shell-sessioner, hvis scriptet er aktiveret.

## Shell-scripts

Et normalt shell-script er beregnet til at blive kaldt i en shell-session i din terminal.
Når det er aktiveret, bliver scriptet kopieret til målsystemet og lagt ind i PATH i alle kompatible shells.
På den måde kan du kalde scriptet fra hvor som helst i en terminalsession.
Scriptnavnet skrives med små bogstaver, og mellemrum erstattes med understregninger, så du nemt kan kalde scriptet.

Hvis du f.eks. opretter et simpelt shell-script med navnet `apti` som
```
sudo apt install "$1"
```
kan du kalde det på ethvert kompatibelt system med `apti.sh <pkg>`, hvis scriptet er aktiveret.

## Fil-scripts

Endelig kan du også køre brugerdefinerede scripts med filinput fra filbrowser-grænsefladen.
Når et filscript er aktiveret, vises det i filbrowseren, så det kan køres med filinput.

Hvis du f.eks. opretter et simpelt filscript som
```
sudo apt install "$@"
```
kan du køre scriptet på udvalgte filer, hvis scriptet er aktiveret.

## Flere typer

Da eksemplet på fil-scriptet er det samme som eksemplet på shell-scriptet ovenfor,
kan du se, at du også kan sætte kryds i flere bokse for udførelsestyper af et script, hvis de skal bruges i flere scenarier.


