## Deteksi jenis cangkang

XPipe bekerja dengan mendeteksi jenis shell dari koneksi dan kemudian berinteraksi dengan shell yang aktif. Namun pendekatan ini hanya berfungsi ketika jenis shell diketahui dan mendukung sejumlah tindakan dan perintah. Semua shell yang umum seperti `bash`, `cmd`, `powershell`, dan banyak lagi, didukung.

## Jenis shell yang tidak dikenal

Jika Anda menyambung ke sistem yang tidak menjalankan command shell yang dikenal, misalnya router, link, atau perangkat IOT, XPipe tidak akan dapat mendeteksi jenis shell dan akan muncul error setelah beberapa waktu. Dengan mengaktifkan opsi ini, XPipe tidak akan mencoba mengidentifikasi jenis shell dan meluncurkan shell apa adanya. Hal ini memungkinkan Anda untuk membuka koneksi tanpa kesalahan, tetapi banyak fitur, misalnya peramban berkas, skrip, subkoneksi, dan banyak lagi, tidak akan didukung untuk koneksi ini.
