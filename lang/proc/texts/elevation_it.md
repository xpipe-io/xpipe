## Elevazione

Il processo di elevazione dei permessi è specifico del sistema operativo.

### Linux e macOS

Qualsiasi comando elevato viene eseguito con `sudo`. La password opzionale `sudo` viene interrogata tramite XPipe quando necessario. Hai la possibilità di regolare il comportamento di elevazione nelle impostazioni per controllare se vuoi inserire la password ogni volta che è necessaria o se vuoi memorizzarla per la sessione corrente.

### Windows

In Windows, non è possibile elevare i permessi di un processo figlio se anche il processo padre non è in esecuzione con permessi elevati. Pertanto, se XPipe non viene eseguito come amministratore, non potrai utilizzare l'elevazione a livello locale. Per le connessioni remote, l'account utente collegato deve avere i privilegi di amministratore.