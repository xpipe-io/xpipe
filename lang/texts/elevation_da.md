## Elevation

Elevationsprocessen er specifik for operativsystemet.

### Linux & macOS

Enhver elevated kommando udføres med `sudo`. Den valgfrie `sudo` adgangskode forespørges via XPipe, når det er nødvendigt.
Du har mulighed for at justere elevation-adfærden i indstillingerne for at kontrollere, om du vil indtaste din adgangskode, hver gang det er nødvendigt, eller om du vil cache den til den aktuelle session.

### Windows

I Windows er det ikke muligt at hæve en underordnet proces, hvis den overordnede proces ikke også er hævet.
Hvis XPipe ikke køres som administrator, vil du derfor ikke kunne bruge nogen elevation lokalt.
For fjernforbindelser skal den tilsluttede brugerkonto have administratorrettigheder.