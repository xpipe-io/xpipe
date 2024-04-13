## Ba?lama

Sa?lad???n?z ba?lama bilgileri do?rudan `ssh` istemcisine ?u ?ekilde iletilir: `-L [origin_address:]origin_port:remote_address:remote_port`.

Varsay?lan olarak, aksi belirtilmedi?i takdirde kaynak geri döngü arayüzüne ba?lanacakt?r. Ayr?ca, IPv4 üzerinden eri?ilebilen tüm a? arayüzlerine ba?lanmak için adresi `0.0.0.0` olarak ayarlamak gibi herhangi bir adres joker karakterinden de yararlanabilirsiniz. Adresi tamamen atlad???n?zda, tüm a? arayüzlerinde ba?lant?lara izin veren `*` joker karakteri kullan?lacakt?r. Baz? a? arayüzleri gösterimlerinin tüm i?letim sistemlerinde desteklenmeyebilece?ini unutmay?n. Örne?in Windows sunucular? `*` joker karakterini desteklemez.
