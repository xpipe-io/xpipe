## Tunnel-binding

De bindingsoplysninger, du angiver, sendes direkte til `ssh`-klienten som følger: `-D [adresse:]port`.

Som standard bindes adressen til loopback-grænsefladen. Du kan også bruge adressejokertegn, f.eks. ved at sætte adressen til `0.0.0.0` for at binde til alle netværksgrænseflader, der er tilgængelige via IPv4. Når du helt udelader adressen, vil jokertegnet `*`, som tillader forbindelser på alle netværksgrænseflader, blive brugt. Bemærk, at nogle notationer for netværksgrænseflader måske ikke understøttes af alle operativsystemer. Windows-servere understøtter f.eks. ikke jokertegnet `*`.
