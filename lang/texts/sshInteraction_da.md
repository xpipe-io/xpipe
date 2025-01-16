## Systeminteraktion

XPipe forsøger at registrere, hvilken slags shell den er logget ind på for at kontrollere, at alt fungerer korrekt, og for at vise systemoplysninger. Det virker for normale kommando-shells som bash, men fejler for ikke-standardiserede og brugerdefinerede login-shells for mange indlejrede systemer. Du er nødt til at deaktivere denne adfærd for at få forbindelser til disse systemer til at lykkes.

Når denne interaktion er deaktiveret, vil den ikke forsøge at identificere nogen systemoplysninger. Det forhindrer, at systemet kan bruges i filbrowseren eller som et proxy/gateway-system til andre forbindelser. XPipe vil så i bund og grund bare fungere som en launcher for forbindelsen.
