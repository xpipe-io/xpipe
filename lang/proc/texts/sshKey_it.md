### Nessuno

Disabilita l'autenticazione a `chiave pubblica`.

### Agente SSH

Se le tue identità sono memorizzate nell'agente SSH, l'eseguibile ssh può utilizzarle se l'agente viene avviato.
XPipe avvierà automaticamente il processo dell'agente se non è ancora in esecuzione.

### Pageant (Windows)

Nel caso in cui si utilizzi pageant su Windows, XPipe verificherà innanzitutto se pageant è in esecuzione.
A causa della natura di pageant, è tua responsabilità averlo in esecuzione
è tua responsabilità che sia in funzione, in quanto dovrai specificare manualmente tutte le chiavi che desideri aggiungere ogni volta.
Se è in funzione, XPipe passerà la pipe con il nome appropriato tramite
`-oIdentityAgent=...` a ssh, non è necessario includere alcun file di configurazione personalizzato.

Si noti che ci sono alcuni bug di implementazione nel client OpenSSH che possono causare dei problemi
se il nome utente contiene spazi o è troppo lungo, quindi cerca di utilizzare la versione più recente.

### Pageant (Linux e macOS)

Se le tue identità sono memorizzate nell'agente pageant, l'eseguibile ssh può utilizzarle se l'agente viene avviato.
XPipe avvierà automaticamente il processo dell'agente se non è ancora in esecuzione.

### File di identità

Puoi anche specificare un file di identità con una passphrase opzionale.
Questa opzione è l'equivalente di `ssh -i <file>`.

Nota che questa deve essere la chiave *privata*, non quella pubblica.
Se fai confusione, ssh ti darà solo messaggi di errore criptici.

### Agente GPG

Se le tue identità sono memorizzate, ad esempio, su una smartcard, puoi scegliere di fornirle al client SSH tramite il `gpg-agent`.
Questa opzione abiliterà automaticamente il supporto SSH dell'agente se non ancora abilitato e riavvierà il demone dell'agente GPG con le impostazioni corrette.

### Yubikey PIV

Se le tue identità sono memorizzate con la funzione smart card PIV di Yubikey, puoi recuperarle con il programma Yubico
con la libreria YKCS11 di Yubico, fornita con Yubico PIV Tool.

Per poter utilizzare questa funzione, è necessario disporre di una versione aggiornata di OpenSSH.

### Agente personalizzato

Puoi anche utilizzare un agente personalizzato fornendo qui la posizione del socket o della named pipe.
Questo verrà passato attraverso l'opzione `IdentityAgent`.

### Libreria PKCS#11 personalizzata

Indica al client OpenSSH di caricare il file di libreria condiviso specificato, che gestirà l'autenticazione.

Si noti che per utilizzare questa funzione è necessaria una versione aggiornata di OpenSSH.
