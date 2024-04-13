## Systeeminteractie

XPipe probeert te detecteren op wat voor soort shell er is ingelogd om te controleren of alles goed werkt en om systeeminformatie weer te geven. Dat werkt voor normale commandoshells zoals bash, maar mislukt voor niet-standaard en aangepaste login-shells voor veel embedded systemen. Je moet dit gedrag uitschakelen om verbindingen met deze systemen te laten slagen.

Als deze interactie is uitgeschakeld, zal het niet proberen om systeeminformatie te identificeren. Dit voorkomt dat het systeem wordt gebruikt in de bestandsbrowser of als een proxy/gateway systeem voor andere verbindingen. XPipe zal dan in wezen alleen fungeren als een launcher voor de verbinding.
