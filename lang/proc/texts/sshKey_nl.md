### Geen

Als deze optie is geselecteerd, levert XPipe geen identiteiten. Hiermee worden ook externe bronnen zoals agenten uitgeschakeld.

### Identiteitsbestand

Je kunt ook een identiteitsbestand opgeven met een optionele wachtwoordzin.
Deze optie is het equivalent van `ssh -i <file>`.

Merk op dat dit de *private* sleutel moet zijn, niet de publieke.
Als je dat verwisselt, zal ssh je alleen maar cryptische foutmeldingen geven.

### SSH-agent

Als je identiteiten zijn opgeslagen in de SSH-Agent, kan de ssh executable deze gebruiken als de agent wordt gestart.
XPipe zal automatisch het agent proces starten als het nog niet draait.

### GPG-agent

Als je identiteiten bijvoorbeeld op een smartcard zijn opgeslagen, kun je ervoor kiezen om deze via de `gpg-agent` aan de SSH-client te verstrekken.
Deze optie schakelt automatisch SSH-ondersteuning van de agent in als deze nog niet is ingeschakeld en herstart de GPG-agent daemon met de juiste instellingen.

### Yubikey PIV

Als je identiteiten zijn opgeslagen met de PIV smartcardfunctie van de Yubikey, dan kun je ze ophalen
ophalen met Yubico's YKCS11 bibliotheek, die wordt meegeleverd met Yubico PIV Tool.

Merk op dat je een up-to-date build van OpenSSH nodig hebt om deze functie te kunnen gebruiken.

### Aangepaste PKCS#11 bibliotheek

Dit zal de OpenSSH client instrueren om het gespecificeerde shared library bestand te laden, dat de authenticatie zal afhandelen.

Merk op dat je een actuele build van OpenSSH nodig hebt om deze functie te gebruiken.

### Pageant (Windows)

Als je pageant op Windows gebruikt, zal XPipe eerst controleren of pageant draait.
Vanwege de aard van pageant, is het jouw verantwoordelijkheid om het
actief is, omdat je elke keer handmatig alle sleutels moet opgeven die je wilt toevoegen.
Als het draait, geeft XPipe de juiste pipe door via
`-oIdentityAgent=...` naar ssh, je hoeft geen aangepaste configuratiebestanden op te nemen.

### Pageant (Linux & macOS)

Als je identiteiten zijn opgeslagen in de pageant agent, kan de ssh executable ze gebruiken als de agent wordt gestart.
XPipe zal automatisch het agent proces starten als het nog niet draait.

### Andere externe bron

Met deze optie kan elke externe identiteitsaanbieder zijn sleutels aan de SSH-client leveren. Je moet deze optie gebruiken als je een andere agent of wachtwoordmanager gebruikt om je SSH-sleutels te beheren.
