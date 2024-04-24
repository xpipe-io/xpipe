## Komut dosyası uyumluluğu

Kabuk türü bu betiğin nerede çalıştırılabileceğini kontrol eder.
Tam eşleşmenin yanı sıra, yani `zsh` betiğini `zsh` içinde çalıştırmanın yanı sıra, XPipe daha geniş bir uyumluluk denetimi de içerecektir.

### Posix Kabukları

`sh` betiği olarak bildirilen herhangi bir betik, `bash` veya `zsh` gibi posix ile ilgili herhangi bir kabuk ortamında çalışabilir.
Temel bir betiği birçok farklı sistemde çalıştırmayı düşünüyorsanız, yalnızca `sh` sözdizimi betiklerini kullanmak bunun için en iyi çözümdür.

### PowerShell

Normal `powershell` komut dosyaları olarak bildirilen komut dosyaları `pwsh` ortamlarında da çalışabilir.
