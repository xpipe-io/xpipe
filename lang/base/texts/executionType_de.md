# Ausführungsarten

Du kannst ein Skript in vielen verschiedenen Szenarien verwenden.

Wenn du ein Skript aktivierst, legen die Ausführungsarten fest, was XPipe mit dem Skript tun soll.

## Init-Skripte

Wenn ein Skript als Init-Skript gekennzeichnet ist, kann es in Shell-Umgebungen ausgewählt werden.

Wenn ein Skript aktiviert ist, wird es außerdem automatisch bei init in allen kompatiblen Shells ausgeführt.

Wenn du zum Beispiel ein einfaches Init-Skript erstellst wie
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
hast du in allen kompatiblen Shell-Sitzungen Zugang zu diesen Aliasen, wenn das Skript aktiviert ist.

## Shell-Skripte

Ein normales Shell-Skript ist dafür gedacht, in einer Shell-Sitzung in deinem Terminal aufgerufen zu werden.
Wenn es aktiviert ist, wird das Skript auf das Zielsystem kopiert und in den PATH aller kompatiblen Shells aufgenommen.
So kannst du das Skript von überall in einer Terminal-Sitzung aufrufen.
Der Skriptname wird kleingeschrieben und Leerzeichen werden durch Unterstriche ersetzt, damit du das Skript leicht aufrufen kannst.

Wenn du zum Beispiel ein einfaches Shell-Skript mit dem Namen `apti` wie folgt erstellst
```
sudo apt install "$1"
```
kannst du das auf jedem kompatiblen System mit `apti.sh <pkg>` aufrufen, wenn das Skript aktiviert ist.

## Datei-Skripte

Schließlich kannst du auch benutzerdefinierte Skripte mit Dateieingaben über die Dateibrowser-Schnittstelle ausführen.
Wenn ein Dateiskript aktiviert ist, wird es im Dateibrowser angezeigt und kann mit Dateieingaben ausgeführt werden.

Wenn du zum Beispiel ein einfaches Dateiskript erstellst wie
```
sudo apt install "$@"
```
kannst du das Skript für ausgewählte Dateien ausführen, wenn das Skript aktiviert ist.

## Mehrere Typen

Da das Beispielskript für die Datei dasselbe ist wie das Beispielsskript für die Shell oben,
siehst du, dass du auch mehrere Kästchen für die Ausführungsarten eines Skripts ankreuzen kannst, wenn sie in mehreren Szenarien verwendet werden sollen.


