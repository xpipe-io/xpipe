## Init-skript

De valfria kommandon som ska köras efter att skalets init-filer och -profiler har exekverats.

Du kan behandla detta som ett vanligt skalskript, dvs. använda all syntax som skalet stöder i skript. Alla kommandon som du utför hämtas av skalet och ändrar miljön. Så om du t.ex. ställer in en variabel kommer du att ha tillgång till variabeln under den här shellsessionen.

### Blockering av kommandon

Observera att blockeringskommandon som kräver användarinmatning kan frysa skalprocessen när XPipe startar upp den internt först i bakgrunden. För att undvika detta ska du bara anropa dessa blockeringskommandon om variabeln `TERM` inte är inställd på `dumb`. XPipe ställer automatiskt in variabeln `TERM=dumb` när den förbereder skalsessionen i bakgrunden och ställer sedan in `TERM=xterm-256color` när terminalen faktiskt öppnas.