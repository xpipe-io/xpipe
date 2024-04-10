## Tipi di esecuzione

Esistono due tipi di esecuzione distinti quando XPipe si connette a un sistema.

### In background

La prima connessione a un sistema avviene in background in una sessione di terminale muta.

I comandi di blocco che richiedono l'input dell'utente possono bloccare il processo di shell quando XPipe lo avvia internamente in background. Per evitare questo problema, dovresti chiamare questi comandi di blocco solo in modalità terminale.

Il navigatore di file, ad esempio, utilizza esclusivamente la modalità di sfondo muta per gestire le sue operazioni, quindi se vuoi che il tuo ambiente di script si applichi alla sessione del navigatore di file, deve essere eseguito in modalità muta.

### Nei terminali

Dopo che la connessione iniziale del terminale muto è riuscita, XPipe aprirà una connessione separata nel terminale vero e proprio. Se vuoi che lo script venga eseguito quando apri la connessione in un terminale, scegli la modalità terminale.
