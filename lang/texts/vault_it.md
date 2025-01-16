# XPipe Git Vault

XPipe può sincronizzare tutti i dati delle tue connessioni con il tuo repository git remoto. Puoi sincronizzarti con questo repository in tutte le istanze dell'applicazione XPipe allo stesso modo: ogni modifica apportata in un'istanza si rifletterà nel repository.

Prima di tutto, devi creare un repository remoto con il tuo provider git preferito. Questo repository deve essere privato.
A questo punto puoi semplicemente copiare e incollare l'URL nell'impostazione del repository remoto di XPipe.

Devi anche avere un client `git` installato localmente sul tuo computer locale. Puoi provare a eseguire `git` in un terminale locale per verificare.
Se non ne hai uno, puoi visitare [https://git-scm.com](https://git-scm.com/) per installare git.

## Autenticazione al repository remoto

Esistono diversi modi per autenticarsi. La maggior parte dei repository utilizza il protocollo HTTPS in cui è necessario specificare un nome utente e una password.
Alcuni provider supportano anche il protocollo SSH, che è supportato anche da XPipe.
Se utilizzi SSH per git, probabilmente sai come configurarlo, quindi questa sezione tratterà solo l'HTTPS.

Devi impostare la tua git CLI in modo che sia in grado di autenticarsi con il repository git remoto tramite HTTPS. Ci sono diversi modi per farlo.
Puoi verificare se è già stato fatto riavviando XPipe una volta configurato un repository remoto.
Se ti chiede le credenziali di accesso, devi impostarle.

Molti strumenti speciali come questo [GitHub CLI](https://cli.github.com/) fanno tutto automaticamente quando vengono installati.
Alcune versioni più recenti del client git possono anche autenticarsi tramite servizi web speciali in cui è sufficiente accedere al proprio account nel browser.

Esistono anche modi manuali per autenticarsi tramite un nome utente e un token.
Al giorno d'oggi, la maggior parte dei provider richiede un token di accesso personale (PAT) per l'autenticazione da riga di comando al posto della tradizionale password.
Puoi trovare le pagine comuni (PAT) qui:
- **GitHub**: [Token di accesso personale (classico)](https://github.com/settings/tokens)
- **GitLab**: [Token di accesso personale](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Token di accesso personale](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Impostazioni -> Applicazioni -> Sezione Gestione dei token di accesso`
Imposta i permessi del token per il repository su Lettura e Scrittura. Gli altri permessi del token possono essere impostati come Lettura.
Anche se il tuo client git ti chiede una password, devi inserire il tuo token a meno che il tuo provider non usi ancora le password.
- La maggior parte dei provider non supporta più le password.

Se non vuoi inserire le tue credenziali ogni volta, puoi utilizzare un qualsiasi gestore di credenziali git.
Per maggiori informazioni, vedi ad esempio:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Alcuni client git moderni si occupano anche di memorizzare automaticamente le credenziali.

Se tutto funziona, XPipe dovrebbe inviare un commit al tuo repository remoto.

## Aggiunta di categorie al repository

Per impostazione predefinita, non vengono impostate categorie di connessioni da sincronizzare, in modo da avere un controllo esplicito su quali connessioni effettuare il commit.
All'inizio, quindi, il tuo repository remoto sarà vuoto.

Per inserire le connessioni di una categoria nel tuo repository git,
devi cliccare sull'icona dell'ingranaggio (quando passi il mouse sulla categoria)
nella scheda `Collegamenti` sotto la panoramica delle categorie sul lato sinistro.
Poi clicca su `Aggiungi al repository git` per sincronizzare la categoria e le connessioni al tuo repository git.
In questo modo tutte le connessioni sincronizzabili verranno aggiunte al repository git.

## Le connessioni locali non vengono sincronizzate

Tutte le connessioni che si trovano sul computer locale non possono essere condivise perché si riferiscono a connessioni e dati che sono disponibili solo sul sistema locale.

Alcune connessioni basate su un file locale, ad esempio le configurazioni SSH, possono essere condivise tramite git se i dati sottostanti, in questo caso il file, sono stati aggiunti al repository git.

## Aggiungere file a git

Quando tutto è pronto, hai la possibilità di aggiungere a git anche altri file, come le chiavi SSH.
Accanto a ogni file scelto c'è un pulsante git che aggiunge il file al repository git.
Anche questi file vengono crittografati quando vengono inviati.
