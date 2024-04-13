## Connessioni shell personalizzate

Apre una shell utilizzando il comando personalizzato eseguendo il comando dato sul sistema host selezionato. Questa shell può essere locale o remota.

Nota che questa funzionalità si aspetta che la shell sia di tipo standard come `cmd`, `bash`, ecc. Se vuoi aprire altri tipi di shell e comandi in un terminale, puoi utilizzare il tipo di comando del terminale personalizzato. L'utilizzo di shell standard ti permette di aprire questa connessione anche nel browser dei file.

### Richiami interattivi

Il processo di shell potrebbe andare in timeout o bloccarsi nel caso in cui si verifichi una richiesta di input inaspettata, come ad esempio una password
inaspettato, come ad esempio la richiesta di una password. Per questo motivo, devi sempre assicurarti che non ci siano richieste interattive di input.

Ad esempio, un comando come `ssh user@host` funzionerà bene se non è richiesta alcuna password.

### Gusci locali personalizzati

In molti casi, è utile lanciare una shell con alcune opzioni che di solito sono disabilitate per impostazione predefinita, in modo da far funzionare correttamente alcuni script e comandi. Ad esempio:

-   [Espansione ritardata in
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Esecuzione di Powershell
    politiche](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Modalità Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- E qualsiasi altra opzione di lancio per una shell di tua scelta

Questo può essere ottenuto creando comandi di shell personalizzati, ad esempio con i seguenti comandi:

-   `cmd /v`
-   `powershell -Modalità di esecuzione Bypass`
-   `bash --posix`