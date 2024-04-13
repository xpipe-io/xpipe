### Configurazioni SSH

XPipe carica tutti gli host e applica tutte le impostazioni che hai configurato nel file selezionato. Pertanto, specificando un'opzione di configurazione su base globale o specifica per un host, questa verrà automaticamente applicata alla connessione stabilita da XPipe.

Se vuoi saperne di più su come usare le configurazioni SSH, puoi usare `man ssh_config` o leggere questa [guida](https://www.ssh.com/academy/ssh/config).

### Identità

Nota che qui puoi anche specificare un'opzione `IdentityFile`. Se qui viene specificata un'identità, qualsiasi altra identità specificata più avanti verrà ignorata.

### Inoltro X11

Se qui viene specificata un'opzione per l'inoltro X11, XPipe tenterà automaticamente di impostare l'inoltro X11 su Windows attraverso WSL.