## Ba?lama

Sa?lad???n?z ba?lama bilgileri do?rudan `ssh` istemcisine ?u ?ekilde aktar?l?r: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Varsay?lan olarak, uzak kaynak adresi geri döngü arayüzüne ba?lanacakt?r. Ayr?ca herhangi bir adres joker karakterini de kullanabilirsiniz, örne?in IPv4 üzerinden eri?ilebilen tüm a? arayüzlerine ba?lanmak için adresi `0.0.0.0` olarak ayarlayabilirsiniz. Adresi tamamen atlad???n?zda, tüm a? arayüzlerinde ba?lant?lara izin veren `*` joker karakteri kullan?lacakt?r. Baz? a? arayüzleri gösterimlerinin tüm i?letim sistemlerinde desteklenmeyebilece?ini unutmay?n. Örne?in Windows sunucular? `*` joker karakterini desteklemez.
