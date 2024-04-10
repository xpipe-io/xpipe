## Inoltro X11

Quando questa opzione è attivata, la connessione SSH verrà avviata con l'inoltro X11 impostato. Su Linux, di solito funziona subito e non richiede alcuna configurazione. Su macOS, è necessario che un server X11 come [XQuartz](https://www.xquartz.org/) sia in esecuzione sul tuo computer locale.

### X11 su Windows

XPipe ti permette di utilizzare le funzionalità X11 di WSL2 per la tua connessione SSH. L'unica cosa di cui hai bisogno è una distribuzione [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) installata sul tuo sistema locale. XPipe sceglierà automaticamente una distribuzione compatibile se possibile, ma puoi anche utilizzarne un'altra nel menu delle impostazioni.

Ciò significa che non è necessario installare un server X11 separato su Windows. Tuttavia, se ne stai utilizzando uno, XPipe lo rileverà e utilizzerà il server X11 attualmente in esecuzione.
