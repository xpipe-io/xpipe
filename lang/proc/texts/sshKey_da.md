### Ingen

Deaktiverer `publickey`-godkendelse.

### SSH-agent

Hvis dine identiteter er gemt i SSH-agenten, kan den eksekverbare ssh-fil bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

### Pageant (Windows)

Hvis du bruger pageant på Windows, vil XPipe først kontrollere, om pageant kører.
På grund af pageants natur er det dit ansvar at få den til at køre, da du
køre, da du manuelt skal angive alle de nøgler, du gerne vil tilføje, hver gang.
Hvis den kører, sender XPipe den rigtige navngivne pipe via
`-oIdentityAgent=...` til ssh, og du behøver ikke at inkludere nogen brugerdefinerede konfigurationsfiler.

Bemærk, at der er nogle implementeringsfejl i OpenSSH-klienten, som kan give problemer
hvis dit brugernavn indeholder mellemrum eller er for langt, så prøv at bruge den nyeste version.

### Pageant (Linux & macOS)

Hvis dine identiteter er gemt i pageant-agenten, kan den eksekverbare ssh-fil bruge dem, hvis agenten startes.
XPipe starter automatisk agentprocessen, hvis den ikke kører endnu.

### Identitetsfil

Du kan også angive en identitetsfil med en valgfri passphrase.
Denne mulighed svarer til `ssh -i <file>`.

Bemærk, at dette skal være den *private* nøgle, ikke den offentlige.
Hvis du blander det sammen, vil ssh kun give dig kryptiske fejlmeddelelser.

### GPG-agent

Hvis dine identiteter f.eks. er gemt på et smartcard, kan du vælge at give dem til SSH-klienten via `gpg-agent`.
Denne indstilling vil automatisk aktivere SSH-understøttelse af agenten, hvis den ikke er aktiveret endnu, og genstarte GPG-agentdæmonen med de korrekte indstillinger.

### Yubikey PIV

Hvis dine identiteter er gemt med Yubikeys PIV-chipkortfunktion, kan du hente dem med Yubicos
dem med Yubicos YKCS11-bibliotek, som følger med Yubico PIV Tool.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.

### Brugerdefineret agent

Du kan også bruge en brugerdefineret agent ved at angive enten socket-placeringen eller den navngivne pipe-placering her.
Dette vil gå via `IdentityAgent`-indstillingen.

### Brugerdefineret PKCS#11-bibliotek

Dette vil instruere OpenSSH-klienten om at indlæse den angivne delte biblioteksfil, som vil håndtere autentificeringen.

Bemærk, at du skal have en opdateret version af OpenSSH for at kunne bruge denne funktion.
