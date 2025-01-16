## Configurazioni SSH

Qui puoi specificare tutte le opzioni SSH da passare alla connessione.
Mentre alcune opzioni sono essenzialmente necessarie per stabilire con successo una connessione, come `HostName`,
molte altre opzioni sono puramente opzionali.

Per avere una panoramica di tutte le opzioni possibili, puoi usare [`man ssh_config`](https://linux.die.net/man/5/ssh_config) o leggere questa [guida](https://www.ssh.com/academy/ssh/config).
L'esatto numero di opzioni supportate dipende esclusivamente dal client SSH installato.

### Formattazione

Il contenuto di questa sezione equivale a una sezione host in un file di configurazione SSH.
Nota che non è necessario definire esplicitamente la chiave `Host`, in quanto ciò verrà fatto automaticamente.

Se intendi definire più di una sezione host, ad esempio con connessioni dipendenti come un host di salto proxy che dipende da un altro host di configurazione, puoi definire più voci host anche qui. XPipe lancerà quindi la prima voce di host.

Non devi eseguire alcuna formattazione con spazi bianchi o indentazione, non è necessario per il funzionamento.

Tieni presente che devi fare attenzione a citare i valori se contengono spazi, altrimenti verranno passati in modo errato.

### File di identità

Nota che qui puoi anche specificare un'opzione `IdentityFile`.
Se questa opzione viene specificata qui, qualsiasi altra opzione di autenticazione basata su chiavi specificata più avanti verrà ignorata.

Se scegli di fare riferimento a un file di identità gestito nel vault git di XPipe, puoi farlo anche tu.
XPipe rileverà i file di identità condivisi e adatterà automaticamente il percorso del file su ogni sistema su cui hai clonato il vault git.
