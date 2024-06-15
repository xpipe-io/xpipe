# Yürütme türleri

Bir komut dosyasını birden fazla farklı senaryoda kullanabilirsiniz.

Bir komut dosyası etkinleştirilirken, yürütme türleri XPipe'ın komut dosyasıyla ne yapacağını belirler.

## Başlangıç betikleri

Bir komut dosyası init komut dosyası olarak belirlendiğinde, kabuk ortamlarında seçilebilir.

Ayrıca, bir betik etkinleştirilirse, tüm uyumlu kabuklarda otomatik olarak init'te çalıştırılacaktır.

Örneğin, aşağıdaki gibi basit bir init betiği oluşturursanız
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
betik etkinleştirilmişse, tüm uyumlu kabuk oturumlarında bu takma adlara erişebileceksiniz.

## Kabuk betikleri

Normal bir kabuk betiği, terminalinizdeki bir kabuk oturumunda çağrılmak üzere tasarlanmıştır.
Etkinleştirildiğinde, betik hedef sisteme kopyalanır ve tüm uyumlu kabuklarda PATH'e yerleştirilir.
Bu, betiği bir terminal oturumunun herhangi bir yerinden çağırmanıza olanak tanır.
Betik adı küçük harflerle yazılır ve boşluklar alt çizgi ile değiştirilir, böylece betiği kolayca çağırabilirsiniz.

Örneğin, `apti` adında aşağıdaki gibi basit bir kabuk betiği oluşturursanız
```
sudo apt install "$1"
```
betik etkinleştirilmişse bunu uyumlu herhangi bir sistemde `apti.sh <pkg>` ile çağırabilirsiniz.

## Dosya komut dosyaları

Son olarak, dosya tarayıcı arayüzünden dosya girdileriyle özel komut dosyası da çalıştırabilirsiniz.
Bir dosya komut dosyası etkinleştirildiğinde, dosya girdileriyle çalıştırılmak üzere dosya tarayıcısında görünecektir.

Örneğin, aşağıdaki gibi basit bir dosya komut dosyası oluşturursanız
```
sudo apt install "$@"
```
komut dosyası etkinleştirilmişse komut dosyasını seçilen dosyalar üzerinde çalıştırabilirsiniz.

## Çoklu tipler

Örnek dosya betiği yukarıdaki örnek kabuk betiği ile aynıdır,
birden fazla senaryoda kullanılmaları gerekiyorsa, bir komut dosyasının yürütme türleri için birden fazla kutuyu da işaretleyebileceğinizi görürsünüz.


