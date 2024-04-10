## Aangepaste shellverbindingen

Opent een commandoregel met het aangepaste commando door het gegeven commando uit te voeren op het geselecteerde hostsysteem. Deze shell kan lokaal of op afstand zijn.

Merk op dat deze functionaliteit verwacht dat de shell van een standaard type is zoals `cmd`, `bash`, enz. Als je andere typen shells en commando's in een terminal wilt openen, kun je in plaats daarvan het aangepaste type terminalcommando gebruiken. Als je standaard shells gebruikt, kun je deze verbinding ook openen in de bestandsbrowser.

### Interactieve prompts

Het shellproces kan uitlopen of hangen als er een onverwachte vereiste invoerprompt is, zoals een wachtwoordprompt
invoerprompt is, zoals een wachtwoordprompt. Daarom moet je er altijd voor zorgen dat er geen interactieve invoerprompts zijn.

Een commando als `ssh user@host` zal hier bijvoorbeeld prima werken zolang er geen wachtwoord vereist is.

### Lokale shells op maat

In veel gevallen is het handig om een shell te starten met bepaalde opties die meestal standaard uitgeschakeld zijn om sommige scripts en commando's goed te laten werken. Bijvoorbeeld:

-   [Vertraagde uitbreiding in
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell uitvoering
    beleid](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Modus](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- En elke andere mogelijke lanceeroptie voor een shell naar keuze

Dit kan worden bereikt door aangepaste shellcommando's te maken met bijvoorbeeld de volgende commando's:

-   `cmd /v`
-   `powershell -Uitvoeringsmodus omzeilen`
-   `bash --posix`