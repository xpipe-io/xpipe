# Exekveringstyper

Du kan använda ett skript i flera olika scenarier.

När du aktiverar ett skript via dess aktiveringsknapp dikterar exekveringstyperna vad XPipe kommer att göra med skriptet.

## Init skript typ

När ett skript betecknas som init-skript kan det väljas i skalmiljöer för att köras vid init.

Om ett skript är aktiverat kommer det dessutom automatiskt att köras på init i alla kompatibla skal.

Om du t.ex. skapar ett enkelt init-skript med
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
du kommer att ha tillgång till dessa alias i alla kompatibla shell-sessioner om skriptet är aktiverat.

## Körbar skripttyp

Ett körbart skalskript är avsett att anropas för en viss anslutning från anslutningshubben.
När det här skriptet är aktiverat kommer skriptet att vara tillgängligt för anrop från skriptknappen för en anslutning med en kompatibel skaldialekt.

Om du t.ex. skapar ett enkelt skalskript med `sh`-dialekt med namnet `ps` för att visa listan över aktuella processer med
```
ps -A
```
kan du anropa skriptet på vilken kompatibel anslutning som helst via skriptmenyn.

## Fil skript typ

Slutligen kan du också köra anpassade skript med filinmatningar från gränssnittet för filbläddraren.
När ett filskript är aktiverat kommer det att visas i filbläddraren för att köras med filinmatningar.

Om du t.ex. skapar ett enkelt filskript med
```
diff "$1" "$2"
```
kan du köra skriptet på valda filer om skriptet är aktiverat.
I det här exemplet kommer skriptet bara att köras framgångsrikt om du har valt exakt två filer.
Annars misslyckas diff-kommandot.

## Shell-session skript typ

Ett sessionsskript är avsett att anropas i en skalsession i din terminal.
När det är aktiverat kommer skriptet att kopieras till målsystemet och läggas till i PATH i alla kompatibla skal.
Detta gör att du kan anropa skriptet från var som helst i en terminalsession.
Skriptnamnet skrivs med små bokstäver och mellanslag ersätts med understreck, så att du enkelt kan anropa skriptet.

Om du till exempel skapar ett enkelt skalskript för `sh`-dialekter med namnet `apti` med
```
sudo apt installera "$1"
```
kan du anropa skriptet på alla kompatibla system med `apti.sh <pkg>` i en terminalsession om skriptet är aktiverat.

## Flera typer

Du kan också kryssa i flera rutor för exekveringstyper för ett skript om de ska användas i flera scenarier.
