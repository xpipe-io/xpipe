## System docelowy VNC

Oprócz normalnych funkcji VNC, XPipe dodaje również dodatkowe funkcje poprzez interakcję z powłoką systemową systemu docelowego.

W kilku przypadkach host serwera VNC, tj. zdalny system, na którym działa serwer VNC, może różnić się od rzeczywistego systemu, który kontrolujesz za pomocą VNC. Na przykład, jeśli serwer VNC jest obsługiwany przez hiperwizor maszyny wirtualnej, taki jak Proxmox, serwer działa na hoście hiperwizora, podczas gdy rzeczywisty system docelowy, który kontrolujesz, na przykład maszyna wirtualna, jest gościem maszyny wirtualnej. Aby upewnić się, że na przykład operacje systemu plików są wykonywane na właściwym systemie, możesz ręcznie zmienić system docelowy, jeśli różni się on od hosta serwera VNC.