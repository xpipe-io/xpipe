# Implementeringer

XPipe uddelegerer den serielle håndtering til eksterne værktøjer.
Der er flere tilgængelige værktøjer, som XPipe kan uddelegere til, hver med deres egne fordele og ulemper.
For at bruge dem kræves det, at de er tilgængelige på værtssystemet.
De fleste muligheder burde være understøttet af alle værktøjer, men nogle mere eksotiske muligheder er det måske ikke.

Før der oprettes forbindelse, kontrollerer XPipe, at det valgte værktøj er installeret og understøtter alle konfigurerede muligheder.
Hvis denne kontrol er vellykket, starter det valgte værktøj.

