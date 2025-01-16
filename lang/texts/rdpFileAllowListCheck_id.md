# Aplikasi jarak jauh RDP

Anda dapat menggunakan koneksi RDP di XPipe untuk meluncurkan aplikasi dan skrip jarak jauh dengan cepat tanpa membuka desktop secara penuh. Namun, karena sifat RDP, Anda harus mengedit daftar izin aplikasi jarak jauh di server Anda agar dapat berfungsi.

## Daftar izin RDP

Server RDP menggunakan konsep daftar izin untuk menangani peluncuran aplikasi. Ini pada dasarnya berarti bahwa kecuali daftar izin dinonaktifkan atau aplikasi tertentu telah secara eksplisit ditambahkan ke dalam daftar izin, peluncuran aplikasi jarak jauh apa pun secara langsung akan gagal.

Anda dapat menemukan pengaturan daftar izin di registri server Anda di `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Mengizinkan semua aplikasi

Anda dapat menonaktifkan daftar izin untuk mengizinkan semua aplikasi jarak jauh dimulai secara langsung dari XPipe. Untuk ini, Anda dapat menjalankan perintah berikut di server Anda di PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Nama "fDisabledAllowList" -Nilai 1`.

### Menambahkan aplikasi yang diizinkan

Sebagai alternatif, Anda juga dapat menambahkan aplikasi jarak jauh satu per satu ke dalam daftar. Ini akan memungkinkan Anda untuk meluncurkan aplikasi yang terdaftar secara langsung dari XPipe.

Di bawah kunci `Applications` dari `TSAppAllowList`, buat kunci baru dengan nama sembarang. Satu-satunya persyaratan untuk nama tersebut adalah bahwa nama tersebut unik di dalam anak kunci "Applications". Kunci baru ini, harus memiliki nilai-nilai ini di dalamnya: `Nama`, `Path` dan `CommandLineSetting`. Anda dapat melakukan ini di PowerShell dengan perintah berikut:

```
$ appName = "Notepad"
$ appPath = "C:\Windows\System32\notepad.exe"

$regKey = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
Item-baru -Path "$regKey\$namaaplikasi"
New-ItemProperty -Path "$regKey\$appName" -Name "Nama" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$namaaplikasi" -Nama "Path" -Nilai "$appPath" -Force
New-ItemProperty -Path "$regKey\$namaaplikasi" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Jika Anda ingin mengizinkan XPipe juga menjalankan skrip dan membuka sesi terminal, Anda harus menambahkan `C:\Windows\System32\cmd.exe` ke dalam daftar yang diijinkan. 

pertimbangan keamanan ## Pertimbangan keamanan

Hal ini tidak membuat server Anda menjadi tidak aman, karena Anda selalu bisa menjalankan aplikasi yang sama secara manual saat meluncurkan koneksi RDP. Daftar izin lebih ditujukan untuk mencegah klien menjalankan aplikasi apa pun secara instan tanpa masukan dari pengguna. Pada akhirnya, terserah Anda apakah Anda mempercayai XPipe untuk melakukan hal ini. Anda bisa meluncurkan koneksi ini dengan baik di luar kotak, ini hanya berguna jika Anda ingin menggunakan salah satu fitur integrasi desktop tingkat lanjut di XPipe.
