### SSH yap?land?rmalar?

XPipe tüm ana bilgisayarlar? yükler ve seçilen dosyada yap?land?rd???n?z tüm ayarlar? uygular. Dolay?s?yla, bir yap?land?rma seçene?ini genel veya ana bilgisayara özel olarak belirtti?inizde, XPipe taraf?ndan kurulan ba?lant?ya otomatik olarak uygulanacakt?r.

SSH yap?land?rmalar?n?n nas?l kullan?laca?? hakk?nda daha fazla bilgi edinmek istiyorsan?z, `man ssh_config` kullanabilir veya bu [k?lavuzu] (https://www.ssh.com/academy/ssh/config) okuyabilirsiniz.

### Kimlikler

Burada bir `IdentityFile` seçene?i de belirtebilece?inizi unutmay?n. Burada herhangi bir kimlik belirtilirse, daha sonra a?a??da belirtilen herhangi bir kimlik göz ard? edilecektir.

### X11 yönlendirme

Burada X11 iletimi için herhangi bir seçenek belirtilirse, XPipe otomatik olarak WSL arac?l???yla Windows üzerinde X11 iletimi kurmaya çal??acakt?r.