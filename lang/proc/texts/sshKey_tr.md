### Yok

`publickey` kimlik doğrulamasını devre dışı bırakır.

### SSH-Agent

Kimliklerinizin SSH-Agent'ta depolanması durumunda, ssh yürütülebilir dosyası, agent başlatıldığında bunları kullanabilir.
XPipe, henüz çalışmıyorsa aracı sürecini otomatik olarak başlatacaktır.

### Pageant (Windows)

Windows üzerinde pageant kullanıyorsanız, XPipe önce pageant'ın çalışıp çalışmadığını kontrol edecektir.
Pageant'ın doğası gereği, pageant'a sahip olmak sizin sorumluluğunuzdadır
her seferinde eklemek istediğiniz tüm anahtarları manuel olarak belirtmeniz gerektiğinden çalışıyor.
Eğer çalışıyorsa, XPipe uygun adlandırılmış boruyu
`-oIdentityAgent=...` ssh için, herhangi bir özel yapılandırma dosyası eklemeniz gerekmez.

OpenSSH istemcisinde sorunlara neden olabilecek bazı uygulama hataları olduğunu unutmayın
kullanıcı adınız boşluk içeriyorsa veya çok uzunsa, en son sürümü kullanmaya çalışın.

### Pageant (Linux ve macOS)

Kimliklerinizin pageant aracısında saklanması durumunda, aracı başlatılırsa ssh yürütülebilir dosyası bunları kullanabilir.
XPipe, henüz çalışmıyorsa aracı sürecini otomatik olarak başlatacaktır.

### Kimlik dosyası

İsteğe bağlı bir parola ile bir kimlik dosyası da belirtebilirsiniz.
Bu seçenek `ssh -i <dosya>` seçeneğine eşdeğerdir.

Bunun genel değil *özel* anahtar olması gerektiğini unutmayın.
Eğer bunu karıştırırsanız, ssh size sadece şifreli hata mesajları verecektir.

### GPG Agent

Kimlikleriniz örneğin bir akıllı kartta saklanıyorsa, bunları SSH istemcisine `gpg-agent` aracılığıyla sağlamayı seçebilirsiniz.
Bu seçenek, henüz etkinleştirilmemişse aracının SSH desteğini otomatik olarak etkinleştirecek ve GPG aracı arka plan programını doğru ayarlarla yeniden başlatacaktır.

### Yubikey PIV

Kimlikleriniz Yubikey'in PIV akıllı kart işlevi ile saklanıyorsa, şunları geri alabilirsiniz
yubico PIV Aracı ile birlikte gelen Yubico'nun YKCS11 kütüphanesi ile.

Bu özelliği kullanabilmek için güncel bir OpenSSH yapısına ihtiyacınız olduğunu unutmayın.

### Özel ajan

Burada soket konumunu veya adlandırılmış boru konumunu sağlayarak özel bir aracı da kullanabilirsiniz.
Bu, `IdentityAgent` seçeneği aracılığıyla aktarılacaktır.

### Özel PKCS#11 kütüphanesi

Bu, OpenSSH istemcisine kimlik doğrulamasını gerçekleştirecek olan belirtilen paylaşılan kütüphane dosyasını yüklemesi talimatını verecektir.

Bu özelliği kullanabilmek için güncel bir OpenSSH yapısına ihtiyacınız olduğunu unutmayın.
