### Ingen

Hvis det er valgt, vil XPipe ikke levere nogen identiteter. Dette deaktiverer også eventuelle eksterne kilder som agenter.

### Identitetsfil

Du kan også angive en identitetsfil med en valgfri passphrase.
Denne mulighed svarer til `ssh -i <file>`.

Bemærk, at dette skal være den *private* nøgle, ikke den offentlige.
Hvis du blander det sammen, vil ssh kun give dig kryptiske fejlmeddelelser.

### SSH-agent

Hvis dine identiteter er gemt i SSH-agenten, kan den eksekverbare ssh bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

### Password manager-agent

Hvis du bruger en adgangskodeadministrator med en SSH-agentfunktion, kan du vælge at bruge den her. XPipe kontrollerer, at den ikke er i konflikt med nogen anden agentkonfiguration. XPipe kan dog ikke starte denne agent af sig selv, du skal sikre dig, at den kører.

### GPG-agent

Hvis dine identiteter f.eks. er gemt på et smartcard, kan du vælge at give dem til SSH-klienten via `gpg-agent`.
Denne mulighed vil automatisk aktivere SSH-understøttelse af agenten, hvis den ikke er aktiveret endnu, og genstarte GPG-agentdæmonen med de korrekte indstillinger.

### Pageant (Windows)

Hvis du bruger pageant på Windows, vil XPipe først kontrollere, om pageant kører.
På grund af pageants natur er det dit ansvar at få den til at køre, da du
køre, da du manuelt skal angive alle de nøgler, du gerne vil tilføje, hver gang.
Hvis den kører, sender XPipe den rigtige navngivne pipe via
`-oIdentityAgent=...` til ssh, og du behøver ikke at inkludere nogen brugerdefinerede konfigurationsfiler.

### Pageant (Linux & macOS)

Hvis dine identiteter er gemt i pageant-agenten, kan den eksekverbare ssh-fil bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

### Yubikey PIV

Hvis dine identiteter er gemt med Yubikeys PIV-chipkortfunktion, kan du hente dem med Yubicos
dem med Yubicos YKCS11-bibliotek, som følger med Yubico PIV Tool.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.

### Brugerdefineret PKCS#11-bibliotek

Dette vil instruere OpenSSH-klienten om at indlæse den angivne delte biblioteksfil, som vil håndtere godkendelsen.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.

### Anden ekstern kilde

Denne indstilling tillader enhver ekstern kørende identitetsudbyder at levere sine nøgler til SSH-klienten. Du bør bruge denne indstilling, hvis du bruger en anden agent eller adgangskodeadministrator til at administrere dine SSH-nøgler.
