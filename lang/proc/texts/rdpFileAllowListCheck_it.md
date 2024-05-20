# Applicazioni remote RDP

Puoi utilizzare le connessioni RDP in XPipe per lanciare rapidamente applicazioni e script remoti senza aprire un desktop completo. Tuttavia, a causa della natura di RDP, devi modificare l'elenco dei permessi per le applicazioni remote sul tuo server affinché questo funzioni.

## Elenchi di permessi RDP

Un server RDP utilizza il concetto di elenchi di permessi per gestire l'avvio delle applicazioni. Questo significa essenzialmente che, a meno che l'elenco dei permessi non sia disabilitato o che non siano state aggiunte esplicitamente applicazioni specifiche all'elenco dei permessi, l'avvio diretto di qualsiasi applicazione remota fallirà.

Puoi trovare le impostazioni dell'elenco di permessi nel registro di sistema del tuo server in `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Consentire tutte le applicazioni

Puoi disabilitare l'elenco dei permessi per consentire l'avvio di tutte le applicazioni remote direttamente da XPipe. A tal fine, puoi eseguire il seguente comando sul tuo server in PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Aggiunta di applicazioni consentite

In alternativa, puoi anche aggiungere singole applicazioni remote all'elenco. In questo modo potrai lanciare le applicazioni elencate direttamente da XPipe.

Sotto la chiave `Applicazioni` di `TSAppAllowList`, crea una nuova chiave con un nome arbitrario. L'unico requisito per il nome è che sia unico tra i figli della chiave "Applications". Questa nuova chiave deve contenere i seguenti valori: `Name`, `Path` e `CommandLineSetting`. Puoi farlo in PowerShell con i seguenti comandi:

```
$appName="Notepad"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applicazioni"
Nuovo elemento -Percorso "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Se vuoi permettere a XPipe di eseguire anche script e aprire sessioni di terminale, devi aggiungere anche `C:\Windows\System32\cmd.exe` all'elenco dei permessi. 

## Considerazioni sulla sicurezza

Questo non rende il tuo server insicuro in alcun modo, poiché puoi sempre eseguire le stesse applicazioni manualmente quando avvii una connessione RDP. Gli elenchi di permessi servono più che altro a evitare che i client eseguano istantaneamente qualsiasi applicazione senza che l'utente la inserisca. In fin dei conti, sta a te decidere se fidarti di XPipe. Puoi lanciare questa connessione senza problemi, ma è utile solo se vuoi utilizzare le funzioni avanzate di integrazione del desktop di XPipe.
