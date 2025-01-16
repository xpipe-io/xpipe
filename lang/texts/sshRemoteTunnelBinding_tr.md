## Bağlama

Sağladığınız bağlama bilgileri doğrudan `ssh` istemcisine şu şekilde aktarılır: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Varsayılan olarak, uzak kaynak adresi geri döngü arayüzüne bağlanacaktır. Ayrıca herhangi bir adres joker karakterini de kullanabilirsiniz, örneğin IPv4 üzerinden erişilebilen tüm ağ arayüzlerine bağlanmak için adresi `0.0.0.0` olarak ayarlayabilirsiniz. Adresi tamamen atladığınızda, tüm ağ arayüzlerinde bağlantılara izin veren `*` joker karakteri kullanılacaktır. Bazı ağ arayüzleri gösterimlerinin tüm işletim sistemlerinde desteklenmeyebileceğini unutmayın. Örneğin Windows sunucuları `*` joker karakterini desteklemez.
