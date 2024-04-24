## X11 Videresendelse

Når denne indstilling er aktiveret, vil SSH-forbindelsen blive startet med X11-videresendelse sat op. På Linux vil dette normalt fungere uden videre og kræver ingen opsætning. På macOS skal du have en X11-server som [XQuartz] (https://www.xquartz.org/) kørende på din lokale maskine.

### X11 på Windows

XPipe giver dig mulighed for at bruge WSL2 X11-funktionerne til din SSH-forbindelse. Det eneste, du behøver til dette, er en [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) distribution installeret på dit lokale system. XPipe vil automatisk vælge en kompatibel installeret distribution, hvis det er muligt, men du kan også bruge en anden i indstillingsmenuen.

Det betyder, at du ikke behøver at installere en separat X11-server på Windows. Men hvis du alligevel bruger en, vil XPipe opdage det og bruge den X11-server, der kører i øjeblikket.
