## Init-script

De kommandoer, der skal køres, efter at shellens init-filer og -profiler er blevet udført.

Du kan behandle dette som et normalt shell-script, dvs. gøre brug af al den syntaks, som shellen understøtter i scripts. Alle kommandoer, du udfører, hentes af shell'en og ændrer miljøet. Så hvis du f.eks. sætter en variabel, vil du have adgang til denne variabel i denne shell-session.

### Blokering af kommandoer

Bemærk, at blokerende kommandoer, der kræver brugerinput, kan fryse shell-processen, når XPipe først starter den op internt i baggrunden. For at undgå dette skal du kun kalde disse blokerende kommandoer, hvor variablen `TERM` ikke er sat til `dumb`. XPipe sætter automatisk variablen `TERM=dumb`, når den forbereder shell-sessionen i baggrunden, og sætter derefter `TERM=xterm-256color`, når terminalen faktisk åbnes.