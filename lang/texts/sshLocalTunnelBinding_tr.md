## Bağlama

Sağladığınız bağlama bilgileri doğrudan `ssh` istemcisine şu şekilde iletilir: `-L [origin_address:]origin_port:remote_address:remote_port`.

Varsayılan olarak, aksi belirtilmediği takdirde kaynak geri döngü arayüzüne bağlanacaktır. Ayrıca, IPv4 üzerinden erişilebilen tüm ağ arayüzlerine bağlanmak için adresi `0.0.0.0` olarak ayarlamak gibi herhangi bir adres joker karakterinden de yararlanabilirsiniz. Adresi tamamen atladığınızda, tüm ağ arayüzlerinde bağlantılara izin veren `*` joker karakteri kullanılacaktır. Bazı ağ arayüzleri gösterimlerinin tüm işletim sistemlerinde desteklenmeyebileceğini unutmayın. Örneğin Windows sunucuları `*` joker karakterini desteklemez.
