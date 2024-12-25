## SSH-konfigurationer

Her kan du angive alle SSH-indstillinger, der skal sendes til forbindelsen.
Mens nogle indstillinger er nødvendige for at etablere en forbindelse, såsom `HostName`,
er mange andre valgmuligheder helt valgfrie.

For at få et overblik over alle mulige indstillinger, kan du bruge [`man ssh_config`] (https://linux.die.net/man/5/ssh_config) eller læse denne [guide] (https://www.ssh.com/academy/ssh/config).
Den nøjagtige mængde af understøttede indstillinger afhænger af din installerede SSH-klient.

### Formatering

Indholdet her svarer til en host-sektion i en SSH-konfigurationsfil.
Bemærk, at du ikke behøver at definere `Host`-nøglen eksplicit, da det vil blive gjort automatisk.

Hvis du har til hensigt at definere mere end én host-sektion, f.eks. med afhængige forbindelser som en proxy jump host, der afhænger af en anden config host, kan du også definere flere host-poster her. XPipe vil derefter starte den første værtspost.

Du behøver ikke at udføre nogen formatering med mellemrum eller indrykning, det er ikke nødvendigt, for at det kan fungere.

Bemærk, at du skal sørge for at citere alle værdier, hvis de indeholder mellemrum, ellers vil de blive sendt forkert.

### Identitetsfiler

Bemærk, at du også kan angive en `IdentityFile`-indstilling her.
Hvis denne indstilling er angivet her, ignoreres enhver ellers angivet nøglebaseret autentificeringsindstilling længere nede.

Hvis du vælger at henvise til en identitetsfil, der administreres i XPipe git vault, kan du også gøre det.
XPipe vil opdage delte identitetsfiler og automatisk tilpasse filstien på hvert system, du har klonet git vault på.
