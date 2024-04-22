## Özel kabuk bağlantıları

Seçilen ana bilgisayar sisteminde verilen komutu çalıştırarak özel komutu kullanarak bir kabuk açar. Bu kabuk yerel ya da uzak olabilir.

Bu işlevin kabuğun `cmd`, `bash`, vb. gibi standart bir türde olmasını beklediğini unutmayın. Bir terminalde başka türde kabuklar ve komutlar açmak istiyorsanız, bunun yerine özel terminal komut türünü kullanabilirsiniz. Standart kabukları kullanmak, bu bağlantıyı dosya tarayıcısında da açmanıza olanak tanır.

### İnteraktif istemler

Beklenmedik bir gereklilik olması durumunda kabuk süreci zaman aşımına uğrayabilir veya askıda kalabilir
giriş istemi, parola istemi gibi. Bu nedenle, her zaman etkileşimli giriş istemleri olmadığından emin olmalısınız.

Örneğin, `ssh user@host` gibi bir komut, parola gerekmediği sürece burada iyi çalışacaktır.

### Özel yerel kabuklar

Birçok durumda, bazı komut dosyalarının ve komutların düzgün çalışmasını sağlamak için genellikle varsayılan olarak devre dışı bırakılan belirli seçeneklerle bir kabuk başlatmak yararlıdır. Örneğin:

-   [Gecikmeli Genişleme
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell yürütme
    policies](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mod](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Ve seçtiğiniz bir kabuk için diğer olası fırlatma seçenekleri

Bu, örneğin aşağıdaki komutlarla özel kabuk komutları oluşturularak gerçekleştirilebilir:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`