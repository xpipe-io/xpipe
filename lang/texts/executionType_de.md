# Ausführungsarten

Du kannst ein Skript in vielen verschiedenen Szenarien verwenden.

Wenn du ein Skript über die Schaltfläche "Aktivieren" aktivierst, legen die Ausführungsarten fest, was XPipe mit dem Skript tun soll.

## Init-Skripttyp

Wenn ein Skript als Init-Skript gekennzeichnet ist, kann es in Shell-Umgebungen ausgewählt werden, um bei Init ausgeführt zu werden.

Wenn ein Skript aktiviert ist, wird es außerdem in allen kompatiblen Shells automatisch bei init ausgeführt.

Wenn du zum Beispiel ein einfaches Init-Skript mit
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
hast du in allen kompatiblen Shell-Sitzungen Zugang zu diesen Aliasen, wenn das Skript aktiviert ist.

## Lauffähiger Skripttyp

Ein lauffähiges Shell-Skript ist dafür gedacht, für eine bestimmte Verbindung vom Verbindungs-Hub aus aufgerufen zu werden.
Wenn dieses Skript aktiviert ist, kann das Skript über die Schaltfläche Skripte für eine Verbindung mit einem kompatiblen Shell-Dialekt aufgerufen werden.

Wenn du zum Beispiel ein einfaches Shell-Skript im `sh`-Dialekt mit dem Namen `ps` erstellst, um die aktuelle Prozessliste anzuzeigen mit
```
ps -A
```
kannst du das Skript auf jeder kompatiblen Verbindung im Menü Skripte aufrufen.

## Datei-Skripttyp

Schließlich kannst du auch benutzerdefinierte Skripte mit Dateieingaben über die Dateibrowser-Schnittstelle ausführen.
Wenn ein Dateiskript aktiviert ist, wird es im Dateibrowser angezeigt und kann mit Dateieingaben ausgeführt werden.

Wenn du zum Beispiel ein einfaches Dateiskript erstellst mit
```
diff "$1" "$2"
```
erstellst, kannst du das Skript für ausgewählte Dateien ausführen, wenn das Skript aktiviert ist.
In diesem Beispiel wird das Skript nur dann erfolgreich ausgeführt, wenn du genau zwei Dateien ausgewählt hast.
Andernfalls wird der Befehl diff fehlschlagen.

## Shell-Sitzung Skripttyp

Ein Sitzungsskript ist dafür gedacht, in einer Shell-Sitzung in deinem Terminal aufgerufen zu werden.
Wenn es aktiviert ist, wird das Skript auf das Zielsystem kopiert und in den PATH aller kompatiblen Shells aufgenommen.
So kannst du das Skript von überall in einer Terminalsitzung aufrufen.
Der Skriptname wird kleingeschrieben und Leerzeichen werden durch Unterstriche ersetzt, damit du das Skript leicht aufrufen kannst.

Wenn du zum Beispiel ein einfaches Shell-Skript für `sh`-Dialekte namens `apti` mit
```
sudo apt install "$1"
```
kannst du das Skript auf jedem kompatiblen System mit `apti.sh <pkg>` in einer Terminalsitzung aufrufen, wenn das Skript aktiviert ist.

## Mehrere Typen

Du kannst auch mehrere Kästchen für die Ausführungsarten eines Skripts ankreuzen, wenn sie in mehreren Szenarien verwendet werden sollen.
