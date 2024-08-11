# Yürütme türleri

Bir komut dosyasını birden fazla farklı senaryoda kullanabilirsiniz.

Bir komut dosyasını etkinleştirme geçiş düğmesi aracılığıyla etkinleştirirken, yürütme türleri XPipe'ın komut dosyasıyla ne yapacağını belirler.

## Başlangıç komut dosyası türü

Bir komut dosyası init komut dosyası olarak belirlendiğinde, init sırasında çalıştırılmak üzere kabuk ortamlarında seçilebilir.

Ayrıca, bir betik etkinleştirilirse, tüm uyumlu kabuklarda otomatik olarak init'te çalıştırılacaktır.

Örneğin, basit bir init betiği oluşturursanız
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
betik etkinleştirilmişse, tüm uyumlu kabuk oturumlarında bu takma adlara erişebileceksiniz.

## Çalıştırılabilir komut dosyası türü

Çalıştırılabilir bir kabuk betiği, bağlantı hub'ından belirli bir bağlantı için çağrılmak üzere tasarlanmıştır.
Bu komut dosyası etkinleştirildiğinde, komut dosyası, uyumlu bir kabuk lehçesine sahip bir bağlantı için komut dosyaları düğmesinden çağrılabilecektir.

Örneğin, geçerli işlem listesini göstermek için `ps` adında basit bir `sh` dialect kabuk betiği oluşturursanız
```
ps -A
```
komut dosyasını komut dosyaları menüsündeki herhangi bir uyumlu bağlantıda çağırabilirsiniz.

## Dosya komut dosyası türü

Son olarak, dosya tarayıcı arayüzünden dosya girdileriyle özel komut dosyası da çalıştırabilirsiniz.
Bir dosya komut dosyası etkinleştirildiğinde, dosya girdileriyle çalıştırılmak üzere dosya tarayıcısında görünecektir.

Örneğin, aşağıdakileri içeren basit bir dosya komut dosyası oluşturursanız
```
diff "$1" "$2"
```
komut dosyası etkinleştirilmişse komut dosyasını seçili dosyalar üzerinde çalıştırabilirsiniz.
Bu örnekte, komut dosyası yalnızca tam olarak iki dosya seçiliyse başarıyla çalışacaktır.
Aksi takdirde, diff komutu başarısız olacaktır.

## Kabuk oturumu komut dosyası türü

Bir oturum betiği, terminalinizdeki bir kabuk oturumunda çağrılmak üzere tasarlanmıştır.
Etkinleştirildiğinde, betik hedef sisteme kopyalanır ve tüm uyumlu kabuklarda PATH'e yerleştirilir.
Bu, betiği bir terminal oturumunun herhangi bir yerinden çağırmanıza olanak tanır.
Betik adı küçük harflerle yazılır ve boşluklar alt çizgilerle değiştirilir, böylece betiği kolayca çağırabilirsiniz.

Örneğin, `sh` lehçeleri için `apti` adında basit bir kabuk betiği oluşturursanız
```
sudo apt install "$1"
```
betik etkinleştirilmişse, betiği herhangi bir uyumlu sistemde terminal oturumunda `apti.sh <pkg>` ile çağırabilirsiniz.

## Çoklu tipler

Birden fazla senaryoda kullanılmaları gerekiyorsa, bir komut dosyasının yürütme türleri için birden fazla kutuyu da işaretleyebilirsiniz.
