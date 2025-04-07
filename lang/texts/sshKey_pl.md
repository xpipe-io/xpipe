### Brak

Jeśli wybierzesz, XPipe nie dostarczy żadnych tożsamości. Powoduje to również wyłączenie wszelkich zewnętrznych źródeł, takich jak agenci.

### Plik tożsamości

Możesz również określić plik tożsamości z opcjonalnym hasłem.
Ta opcja jest odpowiednikiem `ssh -i <file>`.

Zauważ, że powinien to być klucz *prywatny*, a nie publiczny.
Jeśli to pomylisz, ssh da ci tylko tajemnicze komunikaty o błędach.

### SSH-Agent

Jeśli twoje tożsamości są przechowywane w agencie SSH, plik wykonywalny ssh może z nich korzystać, jeśli agent jest uruchomiony.
XPipe automatycznie uruchomi proces agenta, jeśli nie jest on jeszcze uruchomiony.

### Agent menedżera haseł

Jeśli używasz menedżera haseł z funkcją agenta SSH, możesz wybrać go tutaj. XPipe sprawdzi, czy nie koliduje on z żadną inną konfiguracją agenta. XPipe nie może jednak uruchomić tego agenta samodzielnie, musisz upewnić się, że jest on uruchomiony.

### Agent GPG

Jeśli twoje tożsamości są przechowywane na przykład na karcie inteligentnej, możesz udostępnić je klientowi SSH za pośrednictwem `gpg-agent`.
Ta opcja automatycznie włączy obsługę SSH agenta, jeśli nie została jeszcze włączona i ponownie uruchomi demona agenta GPG z prawidłowymi ustawieniami.

### Pageant (Windows)

Jeśli używasz Pageant w systemie Windows, XPipe najpierw sprawdzi, czy Pageant jest uruchomiony.
Ze względu na charakter Pageant, Twoim obowiązkiem jest mieć go uruchomionego
uruchomiony, ponieważ musisz ręcznie określić wszystkie klucze, które chcesz dodać za każdym razem.
Jeśli jest uruchomiony, XPipe przekaże odpowiedni nazwany potok przez
`-oIdentityAgent=...` do ssh, nie musisz dołączać żadnych niestandardowych plików konfiguracyjnych.

### Pageant (Linux i macOS)

W przypadku, gdy twoje tożsamości są przechowywane w agencie Pageant, plik wykonywalny ssh może z nich korzystać, jeśli agent jest uruchomiony.
XPipe automatycznie uruchomi proces agenta, jeśli nie jest on jeszcze uruchomiony.

### Yubikey PIV

Jeśli twoje tożsamości są przechowywane za pomocą funkcji karty inteligentnej PIV w Yubikey, możesz je odzyskać
je za pomocą biblioteki YKCS11 firmy Yubico, która jest dołączona do narzędzia Yubico PIV Tool.

Pamiętaj, że aby korzystać z tej funkcji, potrzebujesz aktualnej wersji OpenSSH.

### Niestandardowa biblioteka PKCS#11

Poinstruuje to klienta OpenSSH, aby załadował określony plik biblioteki współdzielonej, który będzie obsługiwał uwierzytelnianie.

Zauważ, że potrzebujesz aktualnej wersji OpenSSH, aby korzystać z tej funkcji.

### Inne źródło zewnętrzne

Ta opcja pozwoli dowolnemu zewnętrznemu dostawcy tożsamości na dostarczanie kluczy do klienta SSH. Powinieneś użyć tej opcji, jeśli używasz innego agenta lub menedżera haseł do zarządzania kluczami SSH.
