## Binding

Le informazioni sul binding che fornisci vengono passate direttamente al client `ssh` come segue: `-L [indirizzo_origine:]porta_origine:indirizzo_remoto:porta_remoto`.

Per impostazione predefinita, l'origine si legherà all'interfaccia di loopback se non specificato altrimenti. Puoi anche utilizzare i caratteri jolly degli indirizzi, ad esempio impostando l'indirizzo a `0.0.0.0` per effettuare il binding a tutte le interfacce di rete accessibili tramite IPv4. Se ometti completamente l'indirizzo, verrà utilizzato il carattere jolly `*`, che consente le connessioni su tutte le interfacce di rete. Nota che alcune notazioni sulle interfacce di rete potrebbero non essere supportate da tutti i sistemi operativi. I server Windows, ad esempio, non supportano il carattere jolly `*`.
