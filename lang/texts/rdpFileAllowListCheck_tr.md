# RDP uzak uygulamaları

Tam bir masaüstü açmadan uzak uygulamaları ve komut dosyalarını hızlı bir şekilde başlatmak için XPipe'da RDP bağlantılarını kullanabilirsiniz. Ancak, RDP'nin doğası gereği, bunun çalışması için sunucunuzdaki uzak uygulama izin listesini düzenlemeniz gerekir.

## RDP izin listeleri

Bir RDP sunucusu, uygulama başlatma işlemlerini gerçekleştirmek için izin listeleri kavramını kullanır. Bu, izin listesi devre dışı bırakılmadıkça veya belirli uygulamalar açıkça izin listesine eklenmedikçe, herhangi bir uzak uygulamanın doğrudan başlatılmasının başarısız olacağı anlamına gelir.

İzin listesi ayarlarını sunucunuzun kayıt defterinde `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList` adresinde bulabilirsiniz.

### Tüm uygulamalara izin veriliyor

Tüm uzak uygulamaların doğrudan XPipe'dan başlatılmasına izin vermek için izin listesini devre dışı bırakabilirsiniz. Bunun için sunucunuzda PowerShell'de aşağıdaki komutu çalıştırabilirsiniz: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### İzin verilen uygulamaları ekleme

Alternatif olarak, listeye tek tek uzak uygulamalar da ekleyebilirsiniz. Bu sayede listelenen uygulamaları doğrudan XPipe'tan başlatabilirsiniz.

`TSAppAllowList`'in `Applications` anahtarının altında, rastgele bir adla yeni bir anahtar oluşturun. Ad için tek gereklilik, "Uygulamalar" anahtarının alt öğeleri içinde benzersiz olmasıdır. Bu yeni anahtar, içinde şu değerlere sahip olmalıdır: `Name`, `Path` ve `CommandLineSetting`. Bunu PowerShell'de aşağıdaki komutlarla yapabilirsiniz:

```
$appName="Notepad"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

XPipe'ın komut dosyaları çalıştırmasına ve terminal oturumları açmasına da izin vermek istiyorsanız, izin ver listesine `C:\Windows\System32\cmd.exe` dosyasını da eklemeniz gerekir. 

## Güvenlik hususları

Bir RDP bağlantısı başlatırken aynı uygulamaları her zaman manuel olarak çalıştırabileceğiniz için bu, sunucunuzu hiçbir şekilde güvensiz hale getirmez. İzin listeleri daha çok istemcilerin kullanıcı girişi olmadan herhangi bir uygulamayı anında çalıştırmasını önlemeye yöneliktir. Günün sonunda, XPipe'ın bunu yapacağına güvenip güvenmemek size kalmış. Bu bağlantıyı kutudan çıktığı gibi başlatabilirsiniz, bu yalnızca XPipe'daki gelişmiş masaüstü entegrasyon özelliklerinden herhangi birini kullanmak istiyorsanız kullanışlıdır.
