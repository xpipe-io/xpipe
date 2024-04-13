## Detectie Shell-type

XPipe werkt door het shell type van de verbinding te detecteren en dan te interageren met de actieve shell. Deze aanpak werkt echter alleen als het shell type bekend is en een bepaalde hoeveelheid acties en commando's ondersteunt. Alle gangbare shells zoals `bash`, `cmd`, `powershell`, en meer, worden ondersteund.

## Onbekende shell types

Als je verbinding maakt met een systeem dat geen bekende commandoshell draait, bijvoorbeeld een router, link of een IOT-apparaat, dan kan XPipe het shelltype niet detecteren en zal na enige tijd een foutmelding geven. Door deze optie in te schakelen, zal XPipe niet proberen het shell-type te identificeren en de shell as-is starten. Hierdoor kun je de verbinding zonder fouten openen, maar veel functies, zoals de bestandsbrowser, scripts, subverbindingen en meer, worden niet ondersteund voor deze verbinding.
