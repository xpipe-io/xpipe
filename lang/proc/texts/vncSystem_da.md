## VNC-målsystem

Ud over de normale VNC-funktioner tilføjer XPipe også yderligere funktioner gennem interaktion med målsystemets systemskal.

I nogle få tilfælde kan VNC-serverens vært, dvs. det fjernsystem, som VNC-serveren kører på, være forskellig fra det system, du faktisk styrer med VNC. Hvis en VNC-server f.eks. håndteres af en VM-hypervisor som Proxmox, kører serveren på hypervisor-værten, mens det egentlige målsystem, du styrer, f.eks. en VM, er VM-gæsten. For at sikre, at f.eks. filsystemoperationer udføres på det korrekte system, kan du manuelt ændre målsystemet, hvis det er forskelligt fra VNC-serverens vært.