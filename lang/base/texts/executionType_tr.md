## Yürütme türleri

XPipe bir sisteme bağlandığında iki farklı yürütme türü vardır.

### Arka planda

Bir sisteme ilk bağlantı arka planda bir aptal terminal oturumunda yapılır.

Kullanıcı girişi gerektiren engelleme komutları, XPipe arka planda ilk olarak dahili olarak başlatıldığında kabuk sürecini dondurabilir. Bunu önlemek için, bu engelleme komutlarını yalnızca terminal modunda çağırmalısınız.

Örneğin dosya tarayıcısı, işlemlerini gerçekleştirmek için tamamen dilsiz arka plan modunu kullanır; bu nedenle, kod ortamınızın dosya tarayıcısı oturumuna uygulanmasını istiyorsanız, dilsiz modda çalışması gerekir.

### Terminallerde

İlk dumb terminal bağlantısı başarılı olduktan sonra, XPipe gerçek terminalde ayrı bir bağlantı açacaktır. Bağlantıyı bir terminalde açtığınızda komut dosyasının çalıştırılmasını istiyorsanız, terminal modunu seçin.
