## Registrering af shell-type

XPipe fungerer ved at registrere forbindelsens shell-type og derefter interagere med den aktive shell. Denne fremgangsmåde virker dog kun, når shell-typen er kendt og understøtter en vis mængde handlinger og kommandoer. Alle almindelige shells som `bash`, `cmd`, `powershell` og flere understøttes.

## Ukendte shell-typer

Hvis du opretter forbindelse til et system, der ikke kører en kendt kommandoshell, f.eks. en router, et link eller en IOT-enhed, vil XPipe ikke kunne registrere shell-typen og fejle efter et stykke tid. Ved at aktivere denne indstilling vil XPipe ikke forsøge at identificere shell-typen og starte shell'en som den er. Dette giver dig mulighed for at åbne forbindelsen uden fejl, men mange funktioner, f.eks. filbrowseren, scripting, underforbindelser og meget mere, understøttes ikke for denne forbindelse.
