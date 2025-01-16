# XPipe Git Vault

XPipe kan al je verbindingsgegevens synchroniseren met je eigen git remote repository. Je kunt met deze repository synchroniseren in alle XPipe applicatie instanties op dezelfde manier, elke verandering die je maakt in een instantie zal worden weerspiegeld in de repository.

Allereerst moet je een remote repository aanmaken met je favoriete git provider naar keuze. Deze repository moet privé zijn.
Je kunt dan gewoon de URL kopiëren en plakken in de XPipe remote repository instelling.

Je moet ook een lokaal geïnstalleerde `git` client klaar hebben staan op je lokale machine. Je kunt proberen `git` in een lokale terminal te draaien om dit te controleren.
Als je er geen hebt, kun je naar [https://git-scm.com](https://git-scm.com/) gaan om git te installeren.

## Authenticeren naar de remote repository

Er zijn meerdere manieren om te authenticeren. De meeste repositories gebruiken HTTPS waarbij je een gebruikersnaam en wachtwoord moet opgeven.
Sommige providers ondersteunen ook het SSH protocol, dat ook door XPipe wordt ondersteund.
Als je SSH voor git gebruikt, weet je waarschijnlijk hoe je het moet configureren, dus deze sectie zal alleen HTTPS behandelen.

Je moet je git CLI instellen om te kunnen authenticeren met je remote git repository via HTTPS. Er zijn meerdere manieren om dat te doen.
Je kunt controleren of dat al is gedaan door XPipe opnieuw te starten zodra een remote repository is geconfigureerd.
Als het je vraagt om je inloggegevens, dan moet je dat instellen.

Veel speciale tools zoals deze [GitHub CLI](https://cli.github.com/) doen alles automatisch voor je als ze geïnstalleerd zijn.
Sommige nieuwere git client versies kunnen ook authenticeren via speciale webservices waarbij je alleen maar hoeft in te loggen op je account in je browser.

Er zijn ook handmatige manieren om je te authenticeren via een gebruikersnaam en token.
Tegenwoordig vereisen de meeste providers een persoonlijk toegangstoken (PAT) voor authenticatie vanaf de commandoregel in plaats van traditionele wachtwoorden.
Je kunt veelgebruikte (PAT) pagina's hier vinden:
- **GitHub**: [Persoonlijke toegangstokens (klassiek)](https://github.com/settings/tokens)
- **GitLab**: [Persoonlijk toegangstoken](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Persoonlijk toegangstoken](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Instellingen -> Toepassingen -> Sectie Toegangsmunten beheren`
Stel de tokenrechten voor repository in op Lezen en Schrijven. De rest van de tokenrechten kun je instellen als Lezen.
Zelfs als je git client je om een wachtwoord vraagt, moet je je token invoeren, tenzij je provider nog steeds wachtwoorden gebruikt.
- De meeste providers ondersteunen geen wachtwoorden meer.

Als je niet elke keer je referenties wilt invoeren, dan kun je daarvoor elke git credentials manager gebruiken.
Zie voor meer informatie bijvoorbeeld:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Sommige moderne git clients zorgen er ook voor dat de referenties automatisch worden opgeslagen.

Als alles werkt, zou XPipe een commit naar je remote repository moeten pushen.

## Categorieën aan het archief toevoegen

Standaard zijn er geen verbindingscategorieën ingesteld om te synchroniseren, zodat je expliciete controle hebt over welke verbindingen je wilt vastleggen.
Dus in het begin zal je remote repository leeg zijn.

Om je connecties van een categorie in je git repository te plaatsen,
moet je op het tandwiel icoon klikken (als je met je muis over de categorie gaat)
in je `Connecties` tab onder het categorieoverzicht aan de linkerkant.
Klik dan op `Toevoegen aan git repository` om de categorie en verbindingen te synchroniseren met je git repository.
Dit voegt alle synchroniseerbare verbindingen toe aan de git repository.

## Lokale verbindingen worden niet gesynchroniseerd

Elke verbinding die zich onder de lokale machine bevindt kan niet worden gedeeld, omdat het verwijst naar verbindingen en gegevens die alleen beschikbaar zijn op het lokale systeem.

Bepaalde verbindingen die gebaseerd zijn op een lokaal bestand, bijvoorbeeld SSH configs, kunnen gedeeld worden via git als de onderliggende gegevens, in dit geval het bestand, ook aan de git repository zijn toegevoegd.

## Bestanden toevoegen aan git

Als alles is ingesteld, heb je de optie om extra bestanden zoals SSH sleutels ook aan git toe te voegen.
Naast elke bestandskeuze staat een git knop die het bestand zal toevoegen aan de git repository.
Deze bestanden worden ook versleuteld als ze worden gepushed.
