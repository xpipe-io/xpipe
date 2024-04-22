# RDP desktop integration

Du kan bruge denne RDP-forbindelse i XPipe til hurtigt at starte programmer og scripts. På grund af RDP's natur er du dog nødt til at redigere listen over tilladte fjernprogrammer på din server for at få det til at fungere. Desuden gør denne indstilling det muligt at dele drev for at udføre dine scripts på din fjernserver.

Du kan også vælge ikke at gøre dette og bare bruge XPipe til at starte din RDP-klient uden at bruge nogen avancerede desktopintegrationsfunktioner.

## RDP tillader lister

En RDP-server bruger begrebet tilladelseslister til at håndtere programstart. Det betyder i bund og grund, at medmindre tilladelseslisten er deaktiveret, eller specifikke applikationer udtrykkeligt er blevet tilføjet tilladelseslisten, vil direkte start af fjernapplikationer mislykkes.

Du kan finde indstillingerne for tilladelseslisten i registreringsdatabasen på din server under `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Tillader alle programmer

Du kan deaktivere tilladelseslisten for at tillade, at alle fjernprogrammer startes direkte fra XPipe. For at gøre dette kan du køre følgende kommando på din server i PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Tilføjelse af tilladte applikationer

Alternativt kan du også tilføje individuelle fjernprogrammer til listen. Dette vil så give dig mulighed for at starte de anførte applikationer direkte fra XPipe.

Under nøglen `Applications` i `TSAppAllowList` skal du oprette en ny nøgle med et vilkårligt navn. Det eneste krav til navnet er, at det er unikt inden for "Applications"-nøglens børn. Denne nye nøgle skal have disse værdier: `Name`, `Path` og `CommandLineSetting`. Du kan gøre dette i PowerShell med følgende kommandoer:

```
$appName="Notesblok"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applikationer"
Nyt element -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Navn" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
<kode>`</kode>

Hvis du vil tillade XPipe også at køre scripts og åbne terminalsessioner, skal du også tilføje `C:\Windows\System32\cmd.exe` til tilladelseslisten.

## Sikkerhedsovervejelser

Dette gør ikke din server usikker på nogen måde, da du altid kan køre de samme programmer manuelt, når du starter en RDP-forbindelse. Tillad-lister er mere beregnet til at forhindre klienter i øjeblikkeligt at køre ethvert program uden brugerinput. I sidste ende er det op til dig, om du stoler på, at XPipe kan gøre dette. Du kan sagtens starte denne forbindelse uden videre, men det er kun nyttigt, hvis du vil bruge nogle af de avancerede desktop-integrationsfunktioner i XPipe.
