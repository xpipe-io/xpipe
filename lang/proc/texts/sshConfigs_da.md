### SSH-konfigurationer

XPipe indlæser alle værter og anvender alle indstillinger, som du har konfigureret i den valgte fil. Så ved at angive en konfigurationsindstilling på enten en global eller værtsspecifik basis, vil den automatisk blive anvendt på den forbindelse, der oprettes af XPipe.

Hvis du vil vide mere om, hvordan du bruger SSH-konfigurationer, kan du bruge `man ssh_config` eller læse denne [guide] (https://www.ssh.com/academy/ssh/config).

### Identiteter

Bemærk, at du også kan angive en `IdentityFile` her. Hvis en identitet er angivet her, vil enhver anden angivet identitet længere nede blive ignoreret.

### X11-videresendelse

Hvis der angives indstillinger for X11-videresendelse her, vil XPipe automatisk forsøge at opsætte X11-videresendelse på Windows via WSL.