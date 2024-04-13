## SSH yap?land?rmalar?

Burada ba?lant?ya aktar?lmas? gereken SSH seçeneklerini belirtebilirsiniz.
`HostName` gibi baz? seçenekler esasen ba?ar?l? bir ba?lant? kurmak için gereklidir,
di?er birçok seçenek tamamen iste?e ba?l?d?r.

Tüm olas? seçeneklere genel bir bak?? elde etmek için [`man ssh_config`](https://linux.die.net/man/5/ssh_config) adresini kullanabilir veya bu [k?lavuz](https://www.ssh.com/academy/ssh/config) adresini okuyabilirsiniz.
Desteklenen seçeneklerin tam miktar? tamamen kurulu SSH istemcinize ba?l?d?r.

### Biçimlendirme

Buradaki içerik, SSH yap?land?rma dosyas?ndaki bir ana bilgisayar bölümüne e?de?erdir.
`Host` anahtar?n? aç?kça tan?mlamak zorunda olmad???n?z? unutmay?n, çünkü bu otomatik olarak yap?lacakt?r.

Birden fazla ana bilgisayar bölümü tan?mlamak istiyorsan?z, örne?in ba?ka bir yap?land?rma ana bilgisayar?na ba?l? bir proxy atlama ana bilgisayar? gibi ba??ml? ba?lant?lar varsa, burada da birden fazla ana bilgisayar giri?i tan?mlayabilirsiniz. XPipe daha sonra ilk ana bilgisayar giri?ini ba?latacakt?r.

Bo?luk veya girinti ile herhangi bir biçimlendirme yapman?z gerekmez, çal??mas? için buna gerek yoktur.

Bo?luk içeriyorsa herhangi bir de?eri al?nt?lamaya dikkat etmeniz gerekti?ini unutmay?n, aksi takdirde yanl?? aktar?l?rlar.

### Kimlik dosyalar?

Burada bir `IdentityFile` seçene?i de belirtebilece?inizi unutmay?n.
Bu seçenek burada belirtilirse, daha sonra a?a??da belirtilen herhangi bir anahtar tabanl? kimlik do?rulama seçene?i göz ard? edilecektir.

XPipe git kasas?nda yönetilen bir kimlik dosyas?na ba?vurmay? tercih ederseniz, bunu da yapabilirsiniz.
XPipe payla??lan kimlik dosyalar?n? tespit edecek ve git kasas?n? klonlad???n?z her sistemde dosya yolunu otomatik olarak uyarlayacakt?r.
