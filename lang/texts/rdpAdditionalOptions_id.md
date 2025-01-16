# Opsi RDP tambahan

Jika Anda ingin menyesuaikan koneksi lebih lanjut, Anda dapat melakukannya dengan memberikan properti RDP dengan cara yang sama seperti yang terdapat dalam file .rdp. Untuk daftar lengkap properti yang tersedia, lihat [dokumentasi RDP](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Opsi-opsi ini memiliki format `option:type:value`. Jadi misalnya, untuk menyesuaikan ukuran jendela desktop, Anda dapat memberikan konfigurasi berikut ini:
```
desktopwidth:i:*width*
desktopheight:i:*tinggi*
```
