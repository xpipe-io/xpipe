# Identità VM SSH

Se l'utente guest della VM richiede un'autenticazione basata su chiavi per SSH, puoi attivarla qui.

Si presuppone che la VM non sia esposta al pubblico e che il sistema host della VM sia utilizzato come gateway SSH.
Di conseguenza, qualsiasi opzione di identità è specificata relativamente al sistema host della VM e non al tuo computer locale.
Qualsiasi chiave specificata qui viene interpretata come un file sull'host della macchina virtuale.
Se stai usando un agente, è previsto che l'agente sia in esecuzione sul sistema host della VM e non sul tuo computer locale.

### Nessuno

Se selezionato, XPipe non fornirà alcuna identità. Questo disabilita anche qualsiasi fonte esterna come gli agenti.

### File di identità

Puoi anche specificare un file di identità con una passphrase opzionale.
Questa opzione è l'equivalente di `ssh -i <file>`.

Nota che questa deve essere la chiave *privata*, non quella pubblica.
Se fai confusione, ssh ti darà solo messaggi di errore criptici.

### Agente SSH

Se le tue identità sono memorizzate nell'agente SSH, l'eseguibile ssh può utilizzarle se l'agente viene avviato.
XPipe avvierà automaticamente il processo dell'agente se non è ancora in esecuzione.

Se non hai impostato l'agente sul sistema host della macchina virtuale, ti consigliamo di attivare l'inoltro dell'agente SSH per la connessione SSH originale all'host della macchina virtuale.
Puoi farlo creando una connessione SSH personalizzata con l'opzione `ForwardAgent` abilitata.

### Agente GPG

Se le tue identità sono memorizzate, ad esempio, su una smartcard, puoi scegliere di fornirle al client SSH tramite l'opzione `gpg-agent`.
Questa opzione abiliterà automaticamente il supporto SSH dell'agente se non ancora abilitato e riavvierà il demone dell'agente GPG con le impostazioni corrette.

### Yubikey PIV

Se le tue identità sono memorizzate con la funzione smart card PIV di Yubikey, puoi recuperarle con Yubico
con la libreria YKCS11 di Yubico, fornita con Yubico PIV Tool.

Per utilizzare questa funzione è necessario disporre di una versione aggiornata di OpenSSH.

### Libreria PKCS#11 personalizzata

Indica al client OpenSSH di caricare il file di libreria condiviso specificato, che gestirà l'autenticazione.

Si noti che per utilizzare questa funzione è necessaria una versione aggiornata di OpenSSH.

### Pageant (Windows)

Se utilizzi pageant su Windows, XPipe verificherà innanzitutto che pageant sia in esecuzione.
A causa della natura di pageant, è tua responsabilità averlo in esecuzione
è tua responsabilità che sia in funzione, in quanto dovrai specificare manualmente tutte le chiavi che desideri aggiungere ogni volta.
Se è in funzione, XPipe passerà la pipe con il nome appropriato tramite
`-oIdentityAgent=...` a ssh, non è necessario includere alcun file di configurazione personalizzato.

### Pageant (Linux e macOS)

Se le tue identità sono memorizzate nell'agente pageant, l'eseguibile ssh può utilizzarle se l'agente viene avviato.
XPipe avvierà automaticamente il processo dell'agente se non è ancora in esecuzione.

### Altre fonti esterne

Questa opzione consente a qualsiasi provider di identità esterno in esecuzione di fornire le proprie chiavi al client SSH. Dovresti utilizzare questa opzione se stai usando un altro agente o un gestore di password per gestire le chiavi SSH.
