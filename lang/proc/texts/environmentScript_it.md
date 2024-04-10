## Script di avvio

I comandi opzionali da eseguire dopo l'esecuzione dei file e dei profili di init della shell.

Puoi trattarlo come un normale script di shell, cioè utilizzare tutta la sintassi che la shell supporta negli script. Tutti i comandi che esegui sono originati dalla shell e modificano l'ambiente. Quindi, se ad esempio imposti una variabile, avrai accesso a questa variabile in questa sessione di shell.

### Comandi bloccanti

Nota che i comandi bloccanti che richiedono l'input dell'utente possono bloccare il processo di shell quando XPipe lo avvia internamente in background. Per evitare ciò, chiama questi comandi bloccanti solo se la variabile `TERM` non è impostata su `dumb`. XPipe imposta automaticamente la variabile `TERM=dumb` quando prepara la sessione di shell in background e poi imposta `TERM=xterm-256color` quando apre effettivamente il terminale.