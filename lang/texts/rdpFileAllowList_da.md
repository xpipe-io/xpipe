# RDP desktop integration

Du kan bruge denne RDP-forbindelse i XPipe til hurtigt at starte programmer og scripts. På grund af RDP's natur skal du dog redigere listen over tilladte fjernprogrammer på din server for at få det til at fungere. Desuden muliggør denne indstilling drevdeling for at udføre dine scripts på din fjernserver.

Du kan også vælge ikke at gøre dette og bare bruge XPipe til at starte din RDP-klient uden at bruge nogen avancerede desktop-integrationsfunktioner.

## RDP tillader lister

En RDP-server bruger begrebet tilladelseslister til at håndtere programstarter. Det betyder, at medmindre listen over tilladte programmer er deaktiveret, eller specifikke programmer eksplicit er blevet tilføjet listen over tilladte programmer, vil det ikke lykkes at starte fjernprogrammer direkte.

Du kan finde indstillingerne for tilladelseslisten i registreringsdatabasen på din server under `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Tillader alle applikationer

Du kan deaktivere tilladelseslisten for at tillade, at alle fjernprogrammer startes direkte fra XPipe. Til dette kan du køre følgende kommando på din server i PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Tilføjelse af tilladte applikationer

Alternativt kan du også tilføje individuelle fjernprogrammer til listen. Det giver dig mulighed for at starte de anførte programmer direkte fra XPipe.

Under nøglen `Applications` i `TSAppAllowList` skal du oprette en ny nøgle med et vilkårligt navn. Det eneste krav til navnet er, at det er unikt inden for børn af "Applications"-nøglen. Denne nye nøgle skal have disse værdier: `Name`, `Path` og `CommandLineSetting`. Du kan gøre dette i PowerShell med følgende kommandoer:

```
$appName="Notesblok"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
Nyt element -Sti "$regKey\$appName"
New-ItemProperty -Sti "$regKey\$appName" -Navn "Navn" -Værdi "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Hvis du vil tillade XPipe også at køre scripts og åbne terminalsessioner, skal du også tilføje `C:\Windows\System32\cmd.exe` til listen over tilladte filer.

## Sikkerhedsovervejelser

Dette gør ikke din server usikker på nogen måde, da du altid kan køre de samme programmer manuelt, når du starter en RDP-forbindelse. Tilladelseslister er mere beregnet til at forhindre klienter i øjeblikkeligt at køre et hvilket som helst program uden brugerinput. I sidste ende er det op til dig, om du stoler på, at XPipe kan gøre dette. Du kan sagtens starte denne forbindelse uden videre, men det er kun nyttigt, hvis du vil bruge nogle af de avancerede desktop-integrationsfunktioner i XPipe.
