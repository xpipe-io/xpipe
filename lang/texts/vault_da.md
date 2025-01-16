# XPipe Git Vault

XPipe kan synkronisere alle dine forbindelsesdata med dit eget git remote repository. Du kan synkronisere med dette depot i alle XPipe-applikationsforekomster på samme måde, og alle ændringer, du foretager i en forekomst, afspejles i depotet.

Først og fremmest skal du oprette et remote repository med din foretrukne git-udbyder. Dette repository skal være privat.
Derefter kan du bare kopiere og indsætte URL'en i XPipes indstilling for remote repository.

Du skal også have en lokalt installeret `git`-klient klar på din lokale maskine. Du kan prøve at køre `git` i en lokal terminal for at tjekke.
Hvis du ikke har en, kan du besøge [https://git-scm.com](https://git-scm.com/) for at installere git.

## Autentificering til fjerndepotet

Der er flere måder at godkende på. De fleste repositorier bruger HTTPS, hvor du skal angive et brugernavn og en adgangskode.
Nogle udbydere understøtter også SSH-protokollen, som også understøttes af XPipe.
Hvis du bruger SSH til git, ved du sikkert, hvordan du skal konfigurere det, så dette afsnit handler kun om HTTPS.

Du skal sætte din git CLI op til at kunne autentificere med dit eksterne git-repository via HTTPS. Der er flere måder at gøre det på.
Du kan tjekke, om det allerede er gjort, ved at genstarte XPipe, når et fjernlager er konfigureret.
Hvis den beder dig om dine login-oplysninger, skal du sætte dem op.

Mange specialværktøjer som dette [GitHub CLI] (https://cli.github.com/) gør alt automatisk for dig, når det er installeret.
Nogle nyere versioner af git-klienter kan også autentificere via særlige webtjenester, hvor du bare skal logge ind på din konto i din browser.

Der er også manuelle måder at autentificere sig på via et brugernavn og et token.
I dag kræver de fleste udbydere et personligt adgangstoken (PAT) for at godkende fra kommandolinjen i stedet for traditionelle adgangskoder.
Du kan finde almindelige (PAT)-sider her:
- **GitHub**: [Personal access tokens (classic)](https://github.com/settings/tokens)
- **GitLab**: [Personligt adgangstoken](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Personligt adgangstoken](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Indstillinger -> Applikationer -> Administrer adgangstoken`
Indstil tokentilladelsen for repository til Read og Write. Resten af tokentilladelserne kan indstilles som Read.
Selv om din git-klient beder dig om en adgangskode, bør du indtaste dit token, medmindre din udbyder stadig bruger adgangskoder.
- De fleste udbydere understøtter ikke længere adgangskoder.

Hvis du ikke ønsker at indtaste dine legitimationsoplysninger hver gang, kan du bruge en hvilken som helst git-legitimationshåndtering til det.
For mere information, se f.eks:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Nogle moderne git-klienter sørger også for at gemme legitimationsoplysninger automatisk.

Hvis alt fungerer, bør XPipe sende en commit til dit fjernrepository.

## Tilføjelse af kategorier til depotet

Som standard er ingen forbindelseskategorier sat til at synkronisere, så du har eksplicit kontrol over, hvilke forbindelser der skal commit'es.
Så i starten vil dit remote repository være tomt.

For at få dine forbindelser i en kategori lagt ind i dit git-repository,
skal du klikke på tandhjulsikonet (når du holder musen over kategorien)
i fanen `Forbindelser` under kategorioversigten i venstre side.
Klik derefter på `Add to git repository` for at synkronisere kategorien og forbindelserne til dit git-repository.
Dette vil tilføje alle synkroniserbare forbindelser til git-repository'et.

## Lokale forbindelser synkroniseres ikke

Enhver forbindelse, der ligger under den lokale maskine, kan ikke deles, da den henviser til forbindelser og data, der kun er tilgængelige på det lokale system.

Visse forbindelser, der er baseret på en lokal fil, f.eks. SSH-konfigurationer, kan deles via git, hvis de underliggende data, i dette tilfælde filen, også er blevet føjet til git-repository'et.

## Tilføjelse af filer til git

Når alt er sat op, har du mulighed for at tilføje yderligere filer som f.eks. SSH-nøgler til git.
Ved siden af hvert filvalg er der en git-knap, som tilføjer filen til git-repository'et.
Disse filer er også krypterede, når de skubbes.
