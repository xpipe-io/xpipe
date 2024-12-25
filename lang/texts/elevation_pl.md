## Elevation

Proces podniesienia uprawnień jest specyficzny dla systemu operacyjnego.

### Linux i macOS

Każde podwyższone polecenie jest wykonywane z `sudo`. Opcjonalne hasło `sudo` jest w razie potrzeby sprawdzane przez XPipe.
Masz możliwość dostosowania zachowania podwyższania uprawnień w ustawieniach, aby kontrolować, czy chcesz wprowadzać hasło za każdym razem, gdy jest ono potrzebne, czy też chcesz je buforować dla bieżącej sesji.

### Windows

W systemie Windows nie jest możliwe podniesienie poziomu procesu podrzędnego, jeśli proces nadrzędny również nie jest podniesiony.
Dlatego, jeśli XPipe nie jest uruchomiony jako administrator, nie będziesz w stanie użyć żadnego podniesienia lokalnie.
W przypadku połączeń zdalnych połączone konto użytkownika musi mieć uprawnienia administratora.