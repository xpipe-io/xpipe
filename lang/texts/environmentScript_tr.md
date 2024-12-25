## Başlangıç betiği

Kabuğun başlangıç dosyaları ve profilleri yürütüldükten sonra çalıştırılacak isteğe bağlı komutlar.

Buna normal bir kabuk betiği gibi davranabilirsiniz, yani kabuğun betiklerde desteklediği tüm sözdizimini kullanabilirsiniz. Çalıştırdığınız tüm komutlar kabuk tarafından kaynaklanır ve ortamı değiştirir. Dolayısıyla, örneğin bir değişken ayarlarsanız, bu kabuk oturumunda bu değişkene erişiminiz olacaktır.

### Engelleme komutları

Kullanıcı girişi gerektiren engelleme komutlarının, XPipe arka planda ilk olarak dahili olarak başlatıldığında kabuk sürecini dondurabileceğini unutmayın. Bunu önlemek için, bu engelleme komutlarını yalnızca `TERM` değişkeni `dumb` olarak ayarlanmamışsa çağırın. XPipe arka planda kabuk oturumunu hazırlarken `TERM=dumb` değişkenini otomatik olarak ayarlar ve daha sonra terminali gerçekten açarken `TERM=xterm-256color` değişkenini ayarlar.