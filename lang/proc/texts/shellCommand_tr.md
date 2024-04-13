## Özel kabuk ba?lant?lar?

Seçilen ana bilgisayar sisteminde verilen komutu çal??t?rarak özel komutu kullanarak bir kabuk açar. Bu kabuk yerel ya da uzak olabilir.

Bu i?levin kabu?un `cmd`, `bash`, vb. gibi standart bir türde olmas?n? bekledi?ini unutmay?n. Bir terminalde ba?ka türde kabuklar ve komutlar açmak istiyorsan?z, bunun yerine özel terminal komut türünü kullanabilirsiniz. Standart kabuklar? kullanmak, bu ba?lant?y? dosya taray?c?s?nda da açman?za olanak tan?r.

### ?nteraktif istemler

Beklenmedik bir gereklilik olmas? durumunda kabuk süreci zaman a??m?na u?rayabilir veya ask?da kalabilir
giri? istemi, parola istemi gibi. Bu nedenle, her zaman etkile?imli giri? istemleri olmad???ndan emin olmal?s?n?z.

Örne?in, `ssh user@host` gibi bir komut, parola gerekmedi?i sürece burada iyi çal??acakt?r.

### Özel yerel kabuklar

Birçok durumda, baz? komut dosyalar?n?n ve komutlar?n düzgün çal??mas?n? sa?lamak için genellikle varsay?lan olarak devre d??? b?rak?lan belirli seçeneklerle bir kabuk ba?latmak yararl?d?r. Örne?in:

-   [Gecikmeli Geni?leme
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell yürütme
    policies](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mod](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Ve seçti?iniz bir kabuk için di?er olas? f?rlatma seçenekleri

Bu, örne?in a?a??daki komutlarla özel kabuk komutlar? olu?turularak gerçekle?tirilebilir:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`