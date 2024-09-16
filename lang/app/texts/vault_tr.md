# XPipe Git Vault

XPipe, tüm bağlantı verilerinizi kendi git uzak deponuzla senkronize edebilir. Bu depo ile tüm XPipe uygulama örneklerinde aynı şekilde senkronize edebilirsiniz, bir örnekte yaptığınız her değişiklik depoya yansıtılacaktır.

Her şeyden önce, tercih ettiğiniz git sağlayıcısı ile uzak bir depo oluşturmanız gerekir. Bu depo özel olmalıdır.
Daha sonra URL'yi kopyalayıp XPipe uzak depo ayarına yapıştırabilirsiniz.

Ayrıca yerel makinenizde yerel olarak yüklenmiş bir `git` istemcisinin hazır olması gerekir. Kontrol etmek için yerel bir terminalde `git` çalıştırmayı deneyebilirsiniz.
Eğer yoksa, git'i yüklemek için [https://git-scm.com](https://git-scm.com/) adresini ziyaret edebilirsiniz.

## Uzak depoda kimlik doğrulama

Kimlik doğrulamanın birden fazla yolu vardır. Çoğu depo, bir kullanıcı adı ve parola belirtmeniz gereken HTTPS kullanır.
Bazı sağlayıcılar XPipe tarafından da desteklenen SSH protokolünü de destekler.
Eğer git için SSH kullanıyorsanız, muhtemelen nasıl yapılandıracağınızı biliyorsunuzdur, bu yüzden bu bölüm sadece HTTPS'yi kapsayacaktır.

HTTPS aracılığıyla uzak git deponuzla kimlik doğrulaması yapabilmek için git CLI'nızı ayarlamanız gerekir. Bunu yapmanın birden fazla yolu vardır.
Uzak bir depo yapılandırıldıktan sonra XPipe'ı yeniden başlatarak bunun zaten yapılıp yapılmadığını kontrol edebilirsiniz.
Eğer sizden oturum açma kimlik bilgilerinizi isterse, bunu ayarlamanız gerekir.

Bunun gibi birçok özel araç [GitHub CLI] (https://cli.github.com/) yüklendiğinde her şeyi sizin için otomatik olarak yapar.
Bazı yeni git istemci sürümleri, tarayıcınızda hesabınıza giriş yapmanız gereken özel web hizmetleri aracılığıyla da kimlik doğrulaması yapabilir.

Bir kullanıcı adı ve belirteç aracılığıyla kimlik doğrulaması yapmanın manuel yolları da vardır.
Günümüzde çoğu sağlayıcı, geleneksel parolalar yerine komut satırından kimlik doğrulaması için kişisel erişim belirteci (PAT) gerektirmektedir.
Yaygın (PAT) sayfalarını burada bulabilirsiniz:
- **GitHub**: [Kişisel erişim belirteçleri (klasik)](https://github.com/settings/tokens)
- **GitLab**: [Kişisel erişim belirteci](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Kişisel erişim belirteci](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Ayarlar -> Uygulamalar -> Erişim Belirteçlerini Yönet bölümü`
Depo için belirteç iznini Okuma ve Yazma olarak ayarlayın. Token izinlerinin geri kalanı Okuma olarak ayarlanabilir.
Git istemciniz sizden bir parola istese bile, sağlayıcınız hala parola kullanmıyorsa token'ınızı girmelisiniz.
- Çoğu sağlayıcı artık şifreleri desteklemiyor.

Her seferinde kimlik bilgilerinizi girmek istemiyorsanız, bunun için herhangi bir git kimlik bilgileri yöneticisini kullanabilirsiniz.
Daha fazla bilgi için örneğin bkz:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Bazı modern git istemcileri kimlik bilgilerinin otomatik olarak saklanmasını da sağlar.

Her şey yolunda giderse, XPipe uzak deponuza bir commit göndermelidir.

## Depoya kategori ekleme

Varsayılan olarak, hiçbir bağlantı kategorisi senkronize edilecek şekilde ayarlanmamıştır, böylece hangi bağlantıların işleneceği üzerinde açık bir kontrole sahip olursunuz.
Yani başlangıçta uzak deponuz boş olacaktır.

Bir kategorideki bağlantılarınızın git deponuzun içine yerleştirilmesini sağlamak için,
dişli simgesine tıklamanız gerekir (kategorinin üzerine geldiğinizde)
sol taraftaki kategoriye genel bakış altındaki `Bağlantılar` sekmenizde.
Ardından kategoriyi ve bağlantıları git deponuzla senkronize etmek için `Git deposuna ekle` seçeneğine tıklayın.
Bu, senkronize edilebilir tüm bağlantıları git deposuna ekleyecektir.

## Yerel bağlantılar senkronize edilmiyor

Yerel makine altında bulunan herhangi bir bağlantı, yalnızca yerel sistemde bulunan bağlantıları ve verileri ifade ettiğinden paylaşılamaz.

Yerel bir dosyaya dayanan belirli bağlantılar, örneğin SSH yapılandırmaları, altta yatan veriler, bu durumda dosya da git deposuna eklenmişse git aracılığıyla paylaşılabilir.

## git'e dosya ekleme

Her şey ayarlandığında, SSH anahtarları gibi ek dosyaları da git'e ekleme seçeneğiniz vardır.
Her dosya seçiminin yanında, dosyayı git deposuna ekleyecek bir git düğmesi bulunur.
Bu dosyalar itildiğinde de şifrelenir.
