# Implementazioni

XPipe delega la gestione della serialità a strumenti esterni.
Esistono diversi strumenti a cui XPipe può delegare, ognuno con i propri vantaggi e svantaggi.
Per poterli utilizzare, è necessario che siano disponibili sul sistema host.
La maggior parte delle opzioni dovrebbe essere supportata da tutti gli strumenti, ma alcune opzioni più esotiche potrebbero non esserlo.

Prima di connettersi, XPipe verifica che lo strumento selezionato sia installato e che supporti tutte le opzioni configurate.
Se la verifica ha esito positivo, lo strumento selezionato viene avviato.

