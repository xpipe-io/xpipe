## Rilevamento del tipo di shell

XPipe funziona rilevando il tipo di shell della connessione e quindi interagendo con la shell attiva. Questo approccio funziona però solo quando il tipo di shell è noto e supporta un certo numero di azioni e comandi. Sono supportate tutte le shell più comuni come `bash`, `cmd`, `powershell` e altre ancora.

## Tipi di shell sconosciuti

Se ti stai connettendo a un sistema che non esegue una shell di comando nota, ad esempio un router, un link o un dispositivo IOT, XPipe non sarà in grado di rilevare il tipo di shell e darà errore dopo qualche tempo. Abilitando questa opzione, XPipe non tenterà di identificare il tipo di shell e lancerà la shell così com'è. In questo modo potrai aprire la connessione senza errori, ma molte funzioni, come ad esempio il browser di file, lo scripting, le subconnessioni e altro ancora, non saranno supportate per questa connessione.
