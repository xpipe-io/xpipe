## Anpassade shell-anslutningar

Öppnar ett skal med hjälp av det anpassade kommandot genom att utföra det angivna kommandot på det valda värdsystemet. Detta skal kan antingen vara lokalt eller fjärrstyrt.

Observera att den här funktionen förväntar sig att skalet är av standardtyp, t.ex. `cmd`, `bash` osv. Om du vill öppna andra typer av skal och kommandon i en terminal kan du använda kommandotypen anpassad terminal i stället. Om du använder standardskal kan du också öppna den här anslutningen i filbläddraren.

### Interaktiva uppmaningar

Shell-processen kan ta timeout eller hänga sig om det finns en oväntad uppmaning om
inmatning, t.ex. en lösenordsuppmaning. Därför bör du alltid se till att det inte finns några interaktiva inmatningsuppmaningar.

Till exempel fungerar ett kommando som `ssh user@host` bra här så länge det inte krävs något lösenord.

### Anpassade lokala skal

I många fall är det bra att starta ett skal med vissa alternativ som vanligtvis är inaktiverade som standard för att få vissa skript och kommandon att fungera korrekt. Ett exempel:

-   [Fördröjd expansion i
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell-körning
    policyer](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Läge](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Och alla andra möjliga startalternativ för ett skal som du väljer

Detta kan uppnås genom att skapa anpassade skalkommandon med till exempel följande kommandon:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`