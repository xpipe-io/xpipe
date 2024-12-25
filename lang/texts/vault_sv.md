# XPipe Git Vault

XPipe kan synkronisera alla dina anslutningsdata med ditt eget git remote repository. Du kan synkronisera med det här förvaret i alla XPipe-applikationsinstanser på samma sätt, varje ändring du gör i en instans kommer att återspeglas i förvaret.

Först och främst måste du skapa ett fjärrförvar med din favorit git-leverantör som du väljer. Det här förvaret måste vara privat.
Du kan sedan bara kopiera och klistra in webbadressen i XPipe-inställningen för fjärrförvar.

Du måste också ha en lokalt installerad `git`-klient redo på din lokala maskin. Du kan försöka köra `git` i en lokal terminal för att kontrollera.
Om du inte har en sådan kan du besöka [https://git-scm.com](https://git-scm.com/) för att installera git.

## Autentisering till fjärrförvaret

Det finns flera sätt att autentisera. De flesta arkiv använder HTTPS där du måste ange ett användarnamn och lösenord.
Vissa leverantörer stöder också SSH-protokollet, som också stöds av XPipe.
Om du använder SSH för git vet du förmodligen hur du konfigurerar det, så det här avsnittet kommer endast att täcka HTTPS.

Du måste ställa in din git CLI för att kunna autentisera med ditt avlägsna git-förvar via HTTPS. Det finns flera sätt att göra det på.
Du kan kontrollera om det redan är gjort genom att starta om XPipe när ett fjärrförvar är konfigurerat.
Om det ber dig om dina inloggningsuppgifter måste du ställa in det.

Många specialverktyg som detta [GitHub CLI](https://cli.github.com/) gör allt automatiskt åt dig när det installeras.
Vissa nyare git-klientversioner kan också autentisera via speciella webbtjänster där du bara behöver logga in på ditt konto i din webbläsare.

Det finns också manuella sätt att autentisera sig via ett användarnamn och en token.
Numera kräver de flesta leverantörer en personlig åtkomsttoken (PAT) för att autentisera från kommandoraden istället för traditionella lösenord.
Du kan hitta vanliga (PAT) sidor här:
- **GitHub**: [Personliga åtkomsttoken (klassiska)](https://github.com/settings/tokens)
- **GitLab**: [Personlig åtkomsttoken](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Personlig åtkomsttoken](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Inställningar -> Program -> avsnittet Hantera åtkomsttoken`
Ställ in token-behörigheten för repository till Read (läsa) och Write (skriva). Resten av tokenbehörigheterna kan ställas in som Read.
Även om din git-klient uppmanar dig att ange ett lösenord bör du ange din token om inte din leverantör fortfarande använder lösenord.
- De flesta leverantörer stöder inte lösenord längre.

Om du inte vill ange dina autentiseringsuppgifter varje gång kan du använda någon git-autentiseringshanterare för det.
För mer information, se t.ex:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Vissa moderna git-klienter tar också hand om att lagra referenser automatiskt.

Om allt fungerar bör XPipe skjuta en commit till ditt fjärrförvar.

## Lägga till kategorier i förvaret

Som standard är inga anslutningskategorier inställda på synkronisering så att du har uttrycklig kontroll över vilka anslutningar som ska begå.
Så i början kommer ditt fjärrförvar att vara tomt.

För att få dina anslutningar av en kategori placerade i ditt git-arkiv,
måste du klicka på kugghjulsikonen (när du håller muspekaren över kategorin)
på fliken `Connections` under kategoriöversikten på vänster sida.
Klicka sedan på `Add to git repository` för att synkronisera kategorin och anslutningarna till ditt git-repository.
Detta kommer att lägga till alla synkroniserbara anslutningar till git-arkivet.

## Lokala anslutningar synkroniseras inte

Alla anslutningar som finns under den lokala maskinen kan inte delas eftersom de hänvisar till anslutningar och data som endast är tillgängliga på det lokala systemet.

Vissa anslutningar som baseras på en lokal fil, t.ex. SSH-konfigurationer, kan delas via git om de underliggande uppgifterna, i det här fallet filen, också har lagts till i git-arkivet.

## Lägga till filer i git

När allt är klart har du möjlighet att lägga till ytterligare filer, t.ex. SSH-nycklar, i git.
Bredvid varje filval finns en git-knapp som kommer att lägga till filen i git-förvaret.
Dessa filer är också krypterade när de skjuts.
