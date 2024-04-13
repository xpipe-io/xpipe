## Ba?lang?ç beti?i

Kabu?un ba?lang?ç dosyalar? ve profilleri yürütüldükten sonra çal??t?r?lacak iste?e ba?l? komutlar.

Buna normal bir kabuk beti?i gibi davranabilirsiniz, yani kabu?un betiklerde destekledi?i tüm sözdizimini kullanabilirsiniz. Çal??t?rd???n?z tüm komutlar kabuk taraf?ndan kaynaklan?r ve ortam? de?i?tirir. Dolay?s?yla, örne?in bir de?i?ken ayarlarsan?z, bu kabuk oturumunda bu de?i?kene eri?iminiz olacakt?r.

### Engelleme komutlar?

Kullan?c? giri?i gerektiren engelleme komutlar?n?n, XPipe arka planda ilk olarak dahili olarak ba?lat?ld???nda kabuk sürecini dondurabilece?ini unutmay?n. Bunu önlemek için, bu engelleme komutlar?n? yaln?zca `TERM` de?i?keni `dumb` olarak ayarlanmam??sa ça??r?n. XPipe arka planda kabuk oturumunu haz?rlarken `TERM=dumb` de?i?kenini otomatik olarak ayarlar ve daha sonra terminali gerçekten açarken `TERM=xterm-256color` de?i?kenini ayarlar.