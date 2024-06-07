# Tipi di esecuzione

Puoi utilizzare uno script in diversi scenari.

Quando abiliti uno script, i tipi di esecuzione stabiliscono cosa XPipe farà con lo script.

## Script di avvio

Quando uno script è designato come script di avvio, può essere selezionato negli ambienti shell.

Inoltre, se uno script è abilitato, verrà eseguito automaticamente all'avvio in tutte le shell compatibili.

Ad esempio, se crei un semplice script di avvio come
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
avrai accesso a questi alias in tutte le sessioni di shell compatibili se lo script è abilitato.

## Script di shell

Un normale script di shell è destinato a essere richiamato in una sessione di shell nel tuo terminale.
Se abilitato, lo script verrà copiato sul sistema di destinazione e inserito nel PATH di tutte le shell compatibili.
Questo ti permette di richiamare lo script da qualsiasi punto di una sessione di terminale.
Il nome dello script sarà minuscolo e gli spazi saranno sostituiti da trattini bassi, consentendoti di richiamare facilmente lo script.

Ad esempio, se crei un semplice script di shell chiamato `apti` come
```
sudo apt install "$1"
```
puoi richiamarlo su qualsiasi sistema compatibile con `apti.sh <pkg>` se lo script è abilitato.

## File script

Infine, puoi anche eseguire script personalizzati con input da file dall'interfaccia del browser dei file.
Quando uno script di file è abilitato, viene visualizzato nel browser dei file per essere eseguito con input di file.

Ad esempio, se crei un semplice script di file come
```
sudo apt install "$@"
```
puoi eseguire lo script sui file selezionati se lo script è abilitato.

## Tipi multipli

Poiché lo script di esempio per i file è identico allo script di esempio per la shell di cui sopra,
puoi anche spuntare più caselle per i tipi di esecuzione di uno script se questi devono essere utilizzati in più scenari.


