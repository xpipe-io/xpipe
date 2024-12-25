# RDP skrivbordsintegration

Du kan använda den här RDP-anslutningen i XPipe för att snabbt starta applikationer och skript. På grund av RDP:s natur måste du dock redigera listan över tillåtna fjärrprogram på din server för att detta ska fungera. Dessutom möjliggör det här alternativet enhetsdelning för att köra dina skript på din fjärrserver.

Du kan också välja att inte göra detta och bara använda XPipe för att starta din RDP-klient utan att använda några avancerade funktioner för skrivbordsintegration.

## RDP tillåter listor

En RDP-server använder konceptet med tillåtna listor för att hantera applikationslanseringar. Detta innebär i huvudsak att om inte tillåtelselistan är inaktiverad eller specifika applikationer uttryckligen har lagts till i tillåtelselistan, kommer det att misslyckas med att starta fjärrprogram direkt.

Du hittar inställningarna för listan över tillåtna program i registret för din server på `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Tillåter alla program

Du kan inaktivera listan över tillåtna program så att alla fjärrprogram kan startas direkt från XPipe. För detta kan du köra följande kommando på din server i PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Lägga till tillåtna program

Alternativt kan du också lägga till enskilda fjärrprogram i listan. Detta gör att du kan starta de listade applikationerna direkt från XPipe.

Under nyckeln `Applications` i `TSAppAllowList` skapar du en ny nyckel med ett godtyckligt namn. Det enda kravet på namnet är att det är unikt inom barnen till "Applications"-nyckeln. Den nya nyckeln måste innehålla följande värden: `Name`, `Path` och `CommandLineSetting`. Du kan göra detta i PowerShell med följande kommandon:

```
$appName="Anteckningar"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
Nytt objekt -Path "$regKey\$appName"
Ny artikelegenskap - Sökväg "$regKey\$appName" -Namn "Namn" -Värde "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Sökväg "$regKey\$appName" -Namn "CommandLineSetting" -Värde "1" -PropertyType DWord -Force
```

Om du vill tillåta XPipe att även köra skript och öppna terminalsessioner måste du också lägga till `C:\Windows\System32\cmd.exe` i listan över tillåtna filer.

## Säkerhetsöverväganden

Det här gör inte din server osäker på något sätt, eftersom du alltid kan köra samma program manuellt när du startar en RDP-anslutning. Tillåtna listor är mer avsedda att förhindra klienter från att omedelbart köra alla applikationer utan användarinmatning. I slutet av dagen är det upp till dig om du litar på XPipe för att göra detta. Du kan starta den här anslutningen helt fint ur lådan, det här är bara användbart om du vill använda någon av de avancerade skrivbordsintegrationsfunktionerna i XPipe.
