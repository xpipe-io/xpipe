### Ingen

Om det väljs kommer XPipe inte att leverera några identiteter. Detta inaktiverar också alla externa källor som agenter.

### Identitetsfil

Du kan också ange en identitetsfil med en valfri lösenfras.
Det här alternativet motsvarar `ssh -i <file>`.

Observera att detta ska vara den *privata* nyckeln, inte den offentliga.
Om du blandar ihop det kommer ssh bara att ge dig kryptiska felmeddelanden.

### SSH-Agent

Om dina identiteter lagras i SSH-Agent kan den körbara ssh använda dem om agenten startas.
XPipe kommer automatiskt att starta agentprocessen om den inte körs ännu.

### GPG-agent

Om dina identiteter lagras till exempel på ett smartkort kan du välja att tillhandahålla dem till SSH-klienten via `gpg-agent`.
Det här alternativet aktiverar automatiskt SSH-stöd för agenten om det inte är aktiverat ännu och startar om GPG-agentdemonen med rätt inställningar.

### Yubikey PIV

Om dina identiteter lagras med PIV-smartkortsfunktionen i Yubikey kan du hämta dem med Yubicos
med Yubicos YKCS11-bibliotek, som levereras tillsammans med Yubico PIV Tool.

Observera att du behöver en uppdaterad version av OpenSSH för att kunna använda den här funktionen.

### Anpassat PKCS#11-bibliotek

Detta kommer att instruera OpenSSH-klienten att ladda den angivna delade biblioteksfilen, som kommer att hantera autentiseringen.

Observera att du behöver en uppdaterad version av OpenSSH för att kunna använda den här funktionen.

### Pageant (Windows)

Om du använder pageant på Windows kommer XPipe att kontrollera om pageant körs först.
På grund av pageants natur är det ditt ansvar att ha det
körs eftersom du manuellt måste ange alla nycklar du vill lägga till varje gång.
Om det körs kommer XPipe att passera rätt namngivet rör via
`-oIdentityAgent=...` till ssh, du behöver inte inkludera några anpassade konfigurationsfiler.

### Pageant (Linux & macOS)

Om dina identiteter lagras i Pageant-agenten kan den körbara ssh-filen använda dem om agenten startas.
XPipe kommer automatiskt att starta agentprocessen om den inte körs ännu.

### Annan extern källa

Detta alternativ tillåter alla externa körande identitetsleverantörer att leverera sina nycklar till SSH-klienten. Du bör använda det här alternativet om du använder någon annan agent eller lösenordshanterare för att hantera dina SSH-nycklar.
