### Yok

`publickey` kimlik do?rulamas?n? devre d??? b?rak?r.

### SSH-Agent

Kimliklerinizin SSH-Agent'ta depolanmas? durumunda, ssh yürütülebilir dosyas?, agent ba?lat?ld???nda bunlar? kullanabilir.
XPipe, henüz çal??m?yorsa arac? sürecini otomatik olarak ba?latacakt?r.

### Pageant (Windows)

Windows üzerinde pageant kullan?yorsan?z, XPipe önce pageant'?n çal???p çal??mad???n? kontrol edecektir.
Pageant'?n do?as? gere?i, pageant'a sahip olmak sizin sorumlulu?unuzdad?r
her seferinde eklemek istedi?iniz tüm anahtarlar? manuel olarak belirtmeniz gerekti?inden çal???yor.
E?er çal???yorsa, XPipe uygun adland?r?lm?? boruyu
`-oIdentityAgent=...` ssh için, herhangi bir özel yap?land?rma dosyas? eklemeniz gerekmez.

OpenSSH istemcisinde sorunlara neden olabilecek baz? uygulama hatalar? oldu?unu unutmay?n
kullan?c? ad?n?z bo?luk içeriyorsa veya çok uzunsa, en son sürümü kullanmaya çal???n.

### Pageant (Linux ve macOS)

Kimliklerinizin pageant arac?s?nda saklanmas? durumunda, arac? ba?lat?l?rsa ssh yürütülebilir dosyas? bunlar? kullanabilir.
XPipe, henüz çal??m?yorsa arac? sürecini otomatik olarak ba?latacakt?r.

### Kimlik dosyas?

?ste?e ba?l? bir parola ile bir kimlik dosyas? da belirtebilirsiniz.
Bu seçenek `ssh -i <dosya>` seçene?ine e?de?erdir.

Bunun genel de?il *özel* anahtar olmas? gerekti?ini unutmay?n.
E?er bunu kar??t?r?rsan?z, ssh size sadece ?ifreli hata mesajlar? verecektir.

### GPG Agent

Kimlikleriniz örne?in bir ak?ll? kartta saklan?yorsa, bunlar? SSH istemcisine `gpg-agent` arac?l???yla sa?lamay? seçebilirsiniz.
Bu seçenek, henüz etkinle?tirilmemi?se arac?n?n SSH deste?ini otomatik olarak etkinle?tirecek ve GPG arac? arka plan program?n? do?ru ayarlarla yeniden ba?latacakt?r.

### Yubikey PIV

Kimlikleriniz Yubikey'in PIV ak?ll? kart i?levi ile saklan?yorsa, ?unlar? geri alabilirsiniz
yubico PIV Arac? ile birlikte gelen Yubico'nun YKCS11 kütüphanesi ile.

Bu özelli?i kullanabilmek için güncel bir OpenSSH yap?s?na ihtiyac?n?z oldu?unu unutmay?n.

### Özel ajan

Burada soket konumunu veya adland?r?lm?? boru konumunu sa?layarak özel bir arac? da kullanabilirsiniz.
Bu, `IdentityAgent` seçene?i arac?l???yla aktar?lacakt?r.

### Özel PKCS#11 kütüphanesi

Bu, OpenSSH istemcisine kimlik do?rulamas?n? gerçekle?tirecek olan belirtilen payla??lan kütüphane dosyas?n? yüklemesi talimat?n? verecektir.

Bu özelli?i kullanabilmek için güncel bir OpenSSH yap?s?na ihtiyac?n?z oldu?unu unutmay?n.
