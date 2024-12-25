# XPipe Git Vault

XPipe może synchronizować wszystkie twoje dane połączenia z twoim własnym zdalnym repozytorium git. Możesz synchronizować się z tym repozytorium we wszystkich instancjach aplikacji XPipe w ten sam sposób, każda zmiana dokonana w jednej instancji zostanie odzwierciedlona w repozytorium.

Przede wszystkim musisz utworzyć zdalne repozytorium z ulubionym dostawcą git. To repozytorium musi być prywatne.
Następnie możesz po prostu skopiować i wkleić adres URL do ustawień zdalnego repozytorium XPipe.

Musisz również mieć lokalnie zainstalowanego klienta `git` na swoim komputerze lokalnym. Możesz spróbować uruchomić `git` w lokalnym terminalu, aby to sprawdzić.
Jeśli go nie masz, możesz odwiedzić stronę [https://git-scm.com](https://git-scm.com/), aby zainstalować git.

## Uwierzytelnianie do zdalnego repozytorium

Istnieje wiele sposobów uwierzytelniania. Większość repozytoriów używa protokołu HTTPS, w którym musisz podać nazwę użytkownika i hasło.
Niektórzy dostawcy obsługują również protokół SSH, który jest również obsługiwany przez XPipe.
Jeśli używasz SSH dla git, prawdopodobnie wiesz, jak go skonfigurować, więc ta sekcja obejmie tylko HTTPS.

Musisz skonfigurować git CLI, aby móc uwierzytelniać się ze zdalnym repozytorium git przez HTTPS. Możesz to zrobić na wiele sposobów.
Możesz sprawdzić, czy zostało to już zrobione, ponownie uruchamiając XPipe po skonfigurowaniu zdalnego repozytorium.
Jeśli poprosi Cię o podanie danych logowania, musisz je skonfigurować.

Wiele specjalnych narzędzi, takich jak to [GitHub CLI](https://cli.github.com/) robi wszystko automatycznie po zainstalowaniu.
Niektóre nowsze wersje klienta git mogą również uwierzytelniać się za pośrednictwem specjalnych usług internetowych, w których wystarczy zalogować się na swoje konto w przeglądarce.

Istnieją również ręczne sposoby uwierzytelniania za pomocą nazwy użytkownika i tokena.
Obecnie większość dostawców wymaga osobistego tokena dostępu (PAT) do uwierzytelniania z wiersza poleceń zamiast tradycyjnych haseł.
Możesz znaleźć popularne strony (PAT) tutaj:
- **GitHub**: [Personal access tokens (classic)](https://github.com/settings/tokens)
- **GitLab**: [Personal access token](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [osobisty token dostępu](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Ustawienia -> Aplikacje -> Zarządzaj sekcją tokenów dostępu`
Ustaw uprawnienia tokenu dla repozytorium na Odczyt i Zapis. Pozostałe uprawnienia tokenu mogą być ustawione jako Odczyt.
Nawet jeśli twój klient git poprosi cię o hasło, powinieneś wprowadzić swój token, chyba że twój dostawca nadal używa haseł.
- Większość dostawców nie obsługuje już haseł.

Jeśli nie chcesz wprowadzać swoich poświadczeń za każdym razem, możesz użyć do tego dowolnego menedżera poświadczeń git.
Aby uzyskać więcej informacji, zobacz na przykład:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Niektóre nowoczesne klienty git również automatycznie dbają o przechowywanie poświadczeń.

Jeśli wszystko się powiedzie, XPipe powinien wypchnąć commit do twojego zdalnego repozytorium.

## Dodawanie kategorii do repozytorium

Domyślnie żadne kategorie połączeń nie są ustawione na synchronizację, dzięki czemu masz wyraźną kontrolę nad tym, które połączenia mają zostać zatwierdzone.
Tak więc na początku twoje zdalne repozytorium będzie puste.

Aby połączenia kategorii zostały umieszczone w twoim repozytorium git,
musisz kliknąć ikonę koła zębatego (po najechaniu kursorem na kategorię)
w zakładce `Połączenia` w przeglądzie kategorii po lewej stronie.
Następnie kliknij `Dodaj do repozytorium git`, aby zsynchronizować kategorię i połączenia z repozytorium git.
Spowoduje to dodanie wszystkich możliwych do zsynchronizowania połączeń do repozytorium git.

## Połączenia lokalne nie są synchronizowane

Żadne połączenie znajdujące się na komputerze lokalnym nie może być udostępniane, ponieważ odnosi się do połączeń i danych, które są dostępne tylko w systemie lokalnym.

Niektóre połączenia oparte na lokalnym pliku, na przykład konfiguracje SSH, mogą być udostępniane za pośrednictwem git, jeśli podstawowe dane, w tym przypadku plik, zostały również dodane do repozytorium git.

## Dodawanie plików do git

Gdy wszystko jest skonfigurowane, masz możliwość dodania do git również dodatkowych plików, takich jak klucze SSH.
Obok każdego wybranego pliku znajduje się przycisk git, który doda plik do repozytorium git.
Pliki te są również szyfrowane po wypchnięciu.
