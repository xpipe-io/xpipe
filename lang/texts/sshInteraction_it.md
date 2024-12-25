## Interazione con il sistema

XPipe cerca di rilevare il tipo di shell in cui è stato effettuato l'accesso per verificare che tutto funzioni correttamente e per visualizzare le informazioni di sistema. Questo funziona per le normali shell di comando come bash, ma fallisce per le shell di login non standard e personalizzate di molti sistemi embedded. Devi disabilitare questo comportamento affinché le connessioni a questi sistemi vadano a buon fine.

Quando questa interazione è disabilitata, non tenterà di identificare alcuna informazione sul sistema. Questo impedirà al sistema di essere utilizzato nel browser dei file o come sistema proxy/gateway per altre connessioni. XPipe agirà quindi essenzialmente come un lanciatore di connessioni.
