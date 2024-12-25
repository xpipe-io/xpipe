## Interaksi sistem

XPipe mencoba mendeteksi jenis shell yang digunakan untuk masuk untuk memverifikasi bahwa semuanya bekerja dengan benar dan menampilkan informasi sistem. Hal ini berhasil untuk shell perintah normal seperti bash, tetapi gagal untuk shell login non-standar dan kustom untuk banyak sistem tertanam. Anda harus menonaktifkan perilaku ini agar koneksi ke sistem ini berhasil.

Ketika interaksi ini dinonaktifkan, maka tidak akan berusaha mengidentifikasi informasi sistem apa pun. Hal ini akan mencegah sistem untuk digunakan pada peramban berkas atau sebagai sistem proxy/gateway untuk koneksi lain. XPipe pada dasarnya hanya akan bertindak sebagai peluncur untuk koneksi.
