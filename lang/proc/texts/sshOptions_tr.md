## SSH yapılandırmaları

Burada bağlantıya aktarılması gereken SSH seçeneklerini belirtebilirsiniz.
`HostName` gibi bazı seçenekler esasen başarılı bir bağlantı kurmak için gereklidir,
diğer birçok seçenek tamamen isteğe bağlıdır.

Tüm olası seçeneklere genel bir bakış elde etmek için [`man ssh_config`](https://linux.die.net/man/5/ssh_config) adresini kullanabilir veya bu [kılavuz](https://www.ssh.com/academy/ssh/config) adresini okuyabilirsiniz.
Desteklenen seçeneklerin tam miktarı tamamen kurulu SSH istemcinize bağlıdır.

### Biçimlendirme

Buradaki içerik, SSH yapılandırma dosyasındaki bir ana bilgisayar bölümüne eşdeğerdir.
`Host` anahtarını açıkça tanımlamak zorunda olmadığınızı unutmayın, çünkü bu otomatik olarak yapılacaktır.

Birden fazla ana bilgisayar bölümü tanımlamak istiyorsanız, örneğin başka bir yapılandırma ana bilgisayarına bağlı bir proxy atlama ana bilgisayarı gibi bağımlı bağlantılar varsa, burada da birden fazla ana bilgisayar girişi tanımlayabilirsiniz. XPipe daha sonra ilk ana bilgisayar girişini başlatacaktır.

Boşluk veya girinti ile herhangi bir biçimlendirme yapmanız gerekmez, çalışması için buna gerek yoktur.

Boşluk içeriyorsa herhangi bir değeri alıntılamaya dikkat etmeniz gerektiğini unutmayın, aksi takdirde yanlış aktarılırlar.

### Kimlik dosyaları

Burada bir `IdentityFile` seçeneği de belirtebileceğinizi unutmayın.
Bu seçenek burada belirtilirse, daha sonra aşağıda belirtilen herhangi bir anahtar tabanlı kimlik doğrulama seçeneği göz ardı edilecektir.

XPipe git kasasında yönetilen bir kimlik dosyasına başvurmayı tercih ederseniz, bunu da yapabilirsiniz.
XPipe paylaşılan kimlik dosyalarını tespit edecek ve git kasasını klonladığınız her sistemde dosya yolunu otomatik olarak uyarlayacaktır.
