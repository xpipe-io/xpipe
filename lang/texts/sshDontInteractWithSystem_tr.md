## Kabuk tipi algılama

XPipe, bağlantının kabuk türünü algılayarak ve ardından etkin kabukla etkileşime girerek çalışır. Ancak bu yaklaşım yalnızca kabuk türü bilindiğinde ve belirli sayıda eylem ve komutu desteklediğinde çalışır. `bash`, `cmd`, `powershell` ve daha fazlası gibi tüm yaygın kabuklar desteklenir.

## Bilinmeyen kabuk türleri

Bilinen bir komut kabuğu çalıştırmayan bir sisteme bağlanıyorsanız, örneğin bir yönlendirici, bağlantı veya bazı IOT cihazları, XPipe kabuk türünü tespit edemeyecek ve bir süre sonra hata verecektir. Bu seçeneği etkinleştirdiğinizde, XPipe kabuk türünü belirlemeye çalışmaz ve kabuğu olduğu gibi başlatır. Bu, bağlantıyı hatasız açmanıza izin verir, ancak dosya tarayıcısı, komut dosyası oluşturma, alt bağlantılar ve daha fazlası gibi birçok özellik bu bağlantı için desteklenmeyecektir.
