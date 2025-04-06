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

### Agente per la gestione delle password

Se utilizzi un gestore di password con funzionalità di agente SSH, puoi scegliere di utilizzarlo in questa sezione. XPipe verificherà che non sia in conflitto con altre configurazioni dell'agente. XPipe, tuttavia, non può avviare questo agente da solo: devi assicurarti che sia in esecuzione.

### Agente GPG

Se le tue identità sono memorizzate, ad esempio, su una smartcard, puoi scegliere di fornirle al client SSH tramite l'agente `gpg`.
Questa opzione abiliterà automaticamente il supporto SSH dell'agente se non ancora abilitato e riavvierà il demone dell'agente GPG con le impostazioni corrette.

### Pageant (Windows)

Nel caso in cui si utilizzi pageant su Windows, XPipe verificherà innanzitutto che pageant sia in esecuzione.
A causa della natura di pageant, è tua responsabilità averlo in esecuzione
è tua responsabilità che sia in funzione, in quanto dovrai specificare manualmente tutte le chiavi che desideri aggiungere ogni volta.
Se è in funzione, XPipe passerà la pipe con il nome appropriato tramite
`-oIdentityAgent=...` a ssh, non è necessario includere alcun file di configurazione personalizzato.

### Pageant (Linux e macOS)

Se le tue identità sono memorizzate nell'agente pageant, l'eseguibile ssh può utilizzarle se l'agente viene avviato.
XPipe avvierà automaticamente il processo dell'agente se non è ancora in esecuzione.

### Yubikey PIV

Se le tue identità sono memorizzate con la funzione smart card PIV di Yubikey, puoi recuperarle con il programma Yubico
con la libreria YKCS11 di Yubico, fornita con Yubico PIV Tool.

Per poter utilizzare questa funzione, è necessario disporre di una versione aggiornata di OpenSSH.

### Libreria PKCS#11 personalizzata

Indica al client OpenSSH di caricare il file di libreria condiviso specificato, che gestirà l'autenticazione.

Si noti che per utilizzare questa funzione è necessaria una versione aggiornata di OpenSSH.

### Altra fonte esterna

Questa opzione consente a qualsiasi provider di identità esterno in esecuzione di fornire le proprie chiavi al client SSH. Dovresti utilizzare questa opzione se utilizzi un altro agente o un gestore di password per gestire le chiavi SSH.
