# Tipi di esecuzione

Puoi utilizzare uno script in diversi scenari.

Quando abiliti uno script tramite il pulsante di attivazione, i tipi di esecuzione stabiliscono cosa XPipe farà con lo script.

## Tipo di script iniziale

Quando uno script è designato come script di avvio, può essere selezionato negli ambienti shell per essere eseguito all'avvio.

Inoltre, se uno script è abilitato, verrà eseguito automaticamente all'avvio in tutte le shell compatibili.

Ad esempio, se crei un semplice script di avvio con
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
avrai accesso a questi alias in tutte le sessioni di shell compatibili se lo script è abilitato.

## Tipo di script eseguibile

Uno script di shell eseguibile è destinato a essere richiamato per una determinata connessione dall'hub di connessione.
Quando questo script è abilitato, sarà disponibile per essere richiamato dal pulsante script per una connessione con un dialetto shell compatibile.

Ad esempio, se crei un semplice script di shell in dialetto `sh` chiamato `ps` per mostrare l'elenco dei processi correnti con
```
ps -A
```
puoi richiamare lo script su qualsiasi connessione compatibile nel menu degli script.

## Tipo di file script

Infine, puoi anche eseguire script personalizzati con input da file dall'interfaccia del browser dei file.
Quando un file script è abilitato, viene visualizzato nel browser dei file per essere eseguito con gli input dei file.

Ad esempio, se crei un semplice file script con
```
diff "$1" "$2"
```
puoi eseguire lo script sui file selezionati se lo script è abilitato.
In questo esempio, lo script verrà eseguito correttamente solo se sono stati selezionati esattamente due file.
In caso contrario, il comando diff fallirà.

## Tipo di script della sessione di shell

Uno script di sessione è destinato a essere richiamato in una sessione di shell nel tuo terminale.
Se abilitato, lo script verrà copiato sul sistema di destinazione e inserito nel PATH in tutte le shell compatibili.
Questo ti permette di richiamare lo script da qualsiasi punto di una sessione di terminale.
Il nome dello script sarà minuscolo e gli spazi saranno sostituiti da trattini bassi, consentendoti di richiamare facilmente lo script.

Ad esempio, se crei un semplice script di shell per i dialetti `sh` chiamato `apti` con
```
sudo apt install "$1"
```
puoi richiamare lo script su qualsiasi sistema compatibile con `apti.sh <pkg>` in una sessione di terminale se lo script è abilitato.

## Tipi multipli

Puoi anche spuntare più caselle per i tipi di esecuzione di uno script se devono essere utilizzati in più scenari.
