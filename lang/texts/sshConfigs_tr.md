### SSH yapılandırmaları

XPipe tüm ana bilgisayarları yükler ve seçilen dosyada yapılandırdığınız tüm ayarları uygular. Dolayısıyla, bir yapılandırma seçeneğini genel veya ana bilgisayara özel olarak belirttiğinizde, XPipe tarafından kurulan bağlantıya otomatik olarak uygulanacaktır.

SSH yapılandırmalarının nasıl kullanılacağı hakkında daha fazla bilgi edinmek istiyorsanız, `man ssh_config` kullanabilir veya bu [kılavuzu] (https://www.ssh.com/academy/ssh/config) okuyabilirsiniz.

### Kimlikler

Burada bir `IdentityFile` seçeneği de belirtebileceğinizi unutmayın. Burada herhangi bir kimlik belirtilirse, daha sonra aşağıda belirtilen herhangi bir kimlik göz ardı edilecektir.

### X11 yönlendirme

Burada X11 iletimi için herhangi bir seçenek belirtilirse, XPipe otomatik olarak WSL aracılığıyla Windows üzerinde X11 iletimi kurmaya çalışacaktır.