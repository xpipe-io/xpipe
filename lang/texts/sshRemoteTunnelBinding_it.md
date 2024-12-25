## Binding

Le informazioni di binding che fornisci vengono passate direttamente al client `ssh` come segue: `-R [indirizzo_fonte_remoto:]porta_fonte_remoto:indirizzo_destinazione_origine:porta_destinazione_origine`.

Per impostazione predefinita, l'indirizzo sorgente remoto si lega all'interfaccia di loopback. Puoi anche utilizzare dei caratteri jolly, ad esempio impostando l'indirizzo a `0.0.0.0` per effettuare il binding a tutte le interfacce di rete accessibili tramite IPv4. Se ometti completamente l'indirizzo, verrà utilizzato il carattere jolly `*`, che consente le connessioni su tutte le interfacce di rete. Nota che alcune notazioni sulle interfacce di rete potrebbero non essere supportate da tutti i sistemi operativi. I server Windows, ad esempio, non supportano il carattere jolly `*`.
