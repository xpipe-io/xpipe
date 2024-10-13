# VM SSH-identiteter

Hvis din VM-gæstebruger kræver nøglebaseret godkendelse til SSH, kan du aktivere det her.

Bemærk, at det antages, at din VM ikke er eksponeret for offentligheden, så VM-værtssystemet bruges som en SSH-gateway.
Som følge heraf angives enhver identitetsindstilling i forhold til VM-værtssystemet og ikke din lokale maskine.
Enhver nøgle, du angiver her, fortolkes som en fil på VM-værten.
Hvis du bruger en agent, forventes det, at agenten kører på VM-værtssystemet og ikke på din lokale maskine.

### Ingen

Hvis dette er valgt, vil XPipe ikke levere nogen identiteter. Dette deaktiverer også alle eksterne kilder som f.eks. agenter.

### Identitetsfil

Du kan også angive en identitetsfil med en valgfri passphrase.
Denne mulighed svarer til `ssh -i <file>`.

Bemærk, at dette skal være den *private* nøgle, ikke den offentlige.
Hvis du blander det sammen, vil ssh kun give dig kryptiske fejlmeddelelser.

### SSH-agent

Hvis dine identiteter er gemt i SSH-agenten, kan den eksekverbare ssh bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

Hvis du ikke har sat agenten op på VM-værtssystemet, anbefales det, at du aktiverer SSH-agentforwarding for den oprindelige SSH-forbindelse til VM-værten.
Det kan du gøre ved at oprette en brugerdefineret SSH-forbindelse med indstillingen `ForwardAgent` aktiveret.

### GPG-agent

Hvis dine identiteter f.eks. er gemt på et smartcard, kan du vælge at give dem til SSH-klienten via `gpg-agent`.
Denne indstilling vil automatisk aktivere SSH-understøttelse af agenten, hvis den ikke er aktiveret endnu, og genstarte GPG-agentdæmonen med de korrekte indstillinger.

### Yubikey PIV

Hvis dine identiteter er gemt med Yubikeys PIV-chipkortfunktion, kan du hente dem med Yubicos
dem med Yubicos YKCS11-bibliotek, som følger med Yubico PIV Tool.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.

### Brugerdefineret PKCS#11-bibliotek

Dette vil instruere OpenSSH-klienten om at indlæse den angivne delte biblioteksfil, som vil håndtere godkendelsen.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.

### Pageant (Windows)

Hvis du bruger pageant på Windows, vil XPipe først kontrollere, om pageant kører.
På grund af pageants natur er det dit ansvar at have den kørende, da du
køre, da du manuelt skal angive alle de nøgler, du gerne vil tilføje, hver gang.
Hvis den kører, sender XPipe den rigtige navngivne pipe via
`-oIdentityAgent=...` til ssh, og du behøver ikke at inkludere nogen brugerdefinerede konfigurationsfiler.

### Pageant (Linux & macOS)

Hvis dine identiteter er gemt i pageant-agenten, kan den eksekverbare ssh-fil bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

### Andre eksterne kilder

Denne indstilling tillader enhver ekstern kørende identitetsudbyder at levere sine nøgler til SSH-klienten. Du bør bruge denne indstilling, hvis du bruger en anden agent eller adgangskodeadministrator til at administrere dine SSH-nøgler.
