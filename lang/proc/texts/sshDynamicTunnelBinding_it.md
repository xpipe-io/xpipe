## Legame con il tunnel

Le informazioni sul binding fornite vengono passate direttamente al client `ssh` come segue: `-D [indirizzo:]porta`.

Per impostazione predefinita, l'indirizzo si lega all'interfaccia di loopback. Puoi anche utilizzare i caratteri jolly dell'indirizzo, ad esempio impostando l'indirizzo a `0.0.0.0` per effettuare il binding a tutte le interfacce di rete accessibili tramite IPv4. Se ometti completamente l'indirizzo, verrà utilizzato il carattere jolly `*`, che consente le connessioni su tutte le interfacce di rete. Nota che alcune notazioni sulle interfacce di rete potrebbero non essere supportate da tutti i sistemi operativi. I server Windows, ad esempio, non supportano il carattere jolly `*`.
