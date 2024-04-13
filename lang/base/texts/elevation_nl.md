## Hoogte

Het proces van elevatie is besturingssysteem specifiek.

### Linux en macOS

Elk verhoogd commando wordt uitgevoerd met `sudo`. Het optionele `sudo` wachtwoord wordt indien nodig opgevraagd via XPipe.
Je hebt de mogelijkheid om het verheffingsgedrag aan te passen in de instellingen om te bepalen of je je wachtwoord elke keer wilt invoeren als het nodig is of dat je het wilt cachen voor de huidige sessie.

### Windows

In Windows is het niet mogelijk om een kindproces te verheffen als het ouderproces niet ook is verheven.
Als XPipe dus niet als beheerder wordt uitgevoerd, kun je lokaal geen verheffing gebruiken.
Voor verbindingen op afstand moet de verbonden gebruikersaccount beheerdersrechten krijgen.