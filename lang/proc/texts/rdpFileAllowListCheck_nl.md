# RDP-toepassingen op afstand

Je kunt RDP verbindingen in XPipe gebruiken om snel externe toepassingen en scripts te starten zonder een volledig bureaublad te openen. Vanwege de aard van RDP moet je echter de lijst met toegestane externe toepassingen op je server bewerken om dit te laten werken.

## RDP toestaan lijsten

Een RDP server gebruikt het concept van toestemmingslijsten om het starten van applicaties af te handelen. Dit betekent in wezen dat, tenzij de toestemmingslijst is uitgeschakeld of specifieke applicaties expliciet aan de toestemmingslijst zijn toegevoegd, het starten van applicaties op afstand mislukt.

Je kunt de instellingen voor de toestemmingslijst vinden in het register van je server in `HKEY_LOCAL_MACHINE\SOFTWARE\MicrosoftWindows NTCurrentVersion\Terminal Server\TSAppAllowList`.

### Alle toepassingen toestaan

Je kunt de toestaanlijst uitschakelen zodat alle toepassingen op afstand direct vanuit XPipe kunnen worden gestart. Hiervoor kun je het volgende commando uitvoeren op je server in PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\MicrosoftWindows NTCurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Toegestane applicaties toevoegen

Je kunt ook individuele externe toepassingen aan de lijst toevoegen. Hierdoor kun je de opgesomde toepassingen direct vanuit XPipe starten.

Maak onder de `Toepassingen` sleutel van `TSAppAllowList` een nieuwe sleutel met een willekeurige naam. De enige vereiste voor de naam is dat deze uniek is binnen de kinderen van de "Applications" sleutel. Deze nieuwe sleutel moet de volgende waarden bevatten: `Naam`, `Pad` en `CommandLineSetting`. Je kunt dit doen in PowerShell met de volgende commando's:

```
$appName="Kladblok"
$appPath="C:\WindowsSystem32notepad.exe".

$regKey="HKLM:\SOFTWARE\MicrosoftWindows NTCurrentVersion{Terminal Server}AppAllowList{Applications}"
Nieuw-item -Pad "$regKey$appNaam".
New-ItemProperty -Path "$regKey$appName" -Naam "Naam" -Waarde "$appName" -Force
New-ItemProperty -Path "$regKey$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Als je XPipe wilt toestaan om ook scripts uit te voeren en terminalsessies te openen, moet je `C:WindowsSystem32cmd.exe` ook toevoegen aan de toestaanlijst. 

## Beveiligingsoverwegingen

Dit maakt je server op geen enkele manier onveilig, omdat je dezelfde applicaties altijd handmatig kunt uitvoeren als je een RDP-verbinding start. Toegestane lijsten zijn meer bedoeld om te voorkomen dat clients direct een applicatie starten zonder input van de gebruiker. Uiteindelijk is het aan jou of je XPipe vertrouwt om dit te doen. Je kunt deze verbinding gewoon uit de doos starten, dit is alleen handig als je gebruik wilt maken van de geavanceerde functies voor desktopintegratie in XPipe.
