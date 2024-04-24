## Shell-forbindelsesgateways

Hvis denne funktion er aktiveret, åbner XPipe først en shell-forbindelse til gatewayen og derfra en SSH-forbindelse til den angivne vært. Kommandoen `ssh` skal være tilgængelig og findes i `PATH` på din valgte gateway.

### Spring servere over

Denne mekanisme ligner jump-servere, men svarer ikke til dem. Den er helt uafhængig af SSH-protokollen, så du kan bruge en hvilken som helst shell-forbindelse som gateway.

Hvis du er på udkig efter rigtige SSH-jump-servere, måske også i kombination med agent-videresendelse, skal du bruge den brugerdefinerede SSH-forbindelsesfunktionalitet med konfigurationsindstillingen `ProxyJump`.