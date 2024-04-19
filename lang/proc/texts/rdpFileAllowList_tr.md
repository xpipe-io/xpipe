# RDP masaüstü entegrasyonu

Bu RDP ba?lant?s?n? XPipe'da uygulamalar? ve komut dosyalar?n? h?zl? bir ?ekilde ba?latmak için kullanabilirsiniz. Ancak, RDP'nin do?as? gere?i, bunun çal??mas? için sunucunuzdaki uzak uygulama izin listesini düzenlemeniz gerekir. Ayr?ca, bu seçenek uzak sunucunuzda komut dosyalar?n?z? çal??t?rmak için sürücü payla??m?n? etkinle?tirir.

Bunu yapmamay? da seçebilir ve herhangi bir geli?mi? masaüstü entegrasyon özelli?i kullanmadan RDP istemcinizi ba?latmak için sadece XPipe'? kullanabilirsiniz.

## RDP izin listeleri

Bir RDP sunucusu, uygulama ba?latma i?lemlerini gerçekle?tirmek için izin listeleri kavram?n? kullan?r. Bu, izin listesi devre d??? b?rak?lmad?kça veya belirli uygulamalar aç?kça izin listesine eklenmedikçe, herhangi bir uzak uygulaman?n do?rudan ba?lat?lmas?n?n ba?ar?s?z olaca?? anlam?na gelir.

?zin listesi ayarlar?n? sunucunuzun kay?t defterinde `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList` adresinde bulabilirsiniz.

### Tüm uygulamalara izin veriliyor

Tüm uzak uygulamalar?n do?rudan XPipe'dan ba?lat?lmas?na izin vermek için izin listesini devre d??? b?rakabilirsiniz. Bunun için sunucunuzda PowerShell'de a?a??daki komutu çal??t?rabilirsiniz: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### ?zin verilen uygulamalar? ekleme

Alternatif olarak, listeye tek tek uzak uygulamalar da ekleyebilirsiniz. Bu sayede listelenen uygulamalar? do?rudan XPipe'tan ba?latabilirsiniz.

`TSAppAllowList`'in `Applications` anahtar?n?n alt?nda, rastgele bir adla yeni bir anahtar olu?turun. ?sim için tek gereklilik, "Uygulamalar" anahtar?n?n alt anahtarlar? içinde benzersiz olmas?d?r. Bu yeni anahtar, içinde ?u de?erlere sahip olmal?d?r: `Name`, `Path` ve `CommandLineSetting`. Bunu PowerShell'de a?a??daki komutlarla yapabilirsiniz:

```
$appName="Notepad"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

XPipe'?n komut dosyalar? çal??t?rmas?na ve terminal oturumlar? açmas?na da izin vermek istiyorsan?z, `C:\Windows\System32\cmd.exe` dosyas?n? da izin verilenler listesine eklemeniz gerekir.

## Güvenlik hususlar?

Bir RDP ba?lant?s? ba?lat?rken ayn? uygulamalar? her zaman manuel olarak çal??t?rabilece?iniz için bu, sunucunuzu hiçbir ?ekilde güvensiz hale getirmez. ?zin listeleri daha çok istemcilerin kullan?c? giri?i olmadan herhangi bir uygulamay? an?nda çal??t?rmas?n? önlemeye yöneliktir. Günün sonunda, XPipe'?n bunu yapaca??na güvenip güvenmemek size kalm??. Bu ba?lant?y? kutudan ç?kt??? gibi ba?latabilirsiniz, bu yaln?zca XPipe'daki geli?mi? masaüstü entegrasyon özelliklerinden herhangi birini kullanmak istiyorsan?z kullan??l?d?r.
