# Jenis eksekusi

Anda dapat menggunakan skrip dalam beberapa skenario berbeda.

Saat mengaktifkan skrip melalui tombol sakelar aktifkan, jenis eksekusi menentukan apa yang akan dilakukan XPipe dengan skrip tersebut.

## Jenis skrip inisialisasi

Ketika skrip ditetapkan sebagai skrip init, skrip tersebut dapat dipilih di lingkungan shell untuk dijalankan pada init.

Lebih jauh lagi, jika skrip diaktifkan, skrip tersebut akan secara otomatis dijalankan pada init di semua shell yang kompatibel.

Sebagai contoh, jika Anda membuat skrip init sederhana dengan
```
alias ll = "ls -l"
alias la = "ls -A"
alias l = "ls -CF"
```
anda akan memiliki akses ke alias ini di semua sesi shell yang kompatibel jika skrip diaktifkan.

## Jenis skrip yang dapat dijalankan

Skrip shell yang dapat dijalankan dimaksudkan untuk dipanggil untuk koneksi tertentu dari hub koneksi.
Ketika skrip ini diaktifkan, skrip akan tersedia untuk dipanggil dari tombol skrip untuk koneksi dengan dialek shell yang kompatibel.

Misalnya, jika Anda membuat skrip shell dialek `sh` sederhana bernama `ps` untuk menampilkan daftar proses saat ini dengan
```
ps -A
```
anda dapat memanggil skrip tersebut pada koneksi yang kompatibel di menu skrip.

## Jenis skrip file

Terakhir, Anda juga dapat menjalankan skrip khusus dengan input file dari antarmuka peramban file.
Ketika skrip file diaktifkan, skrip tersebut akan muncul di peramban file untuk dijalankan dengan input file.

Sebagai contoh, jika Anda membuat skrip file sederhana dengan
```
diff "$1" "$2"
```
anda dapat menjalankan skrip pada file yang dipilih jika skrip diaktifkan.
Pada contoh ini, skrip hanya akan berhasil dijalankan jika Anda memiliki dua file yang dipilih.
Jika tidak, perintah diff akan gagal.

jenis skrip sesi shell ## Jenis skrip sesi shell

Skrip sesi dimaksudkan untuk dipanggil pada sesi shell di terminal Anda.
Ketika diaktifkan, skrip akan disalin ke sistem target dan dimasukkan ke dalam PATH pada semua shell yang kompatibel.
Hal ini memungkinkan Anda untuk memanggil skrip dari mana saja dalam sesi terminal.
Nama skrip akan menggunakan huruf kecil dan spasi akan diganti dengan garis bawah, sehingga Anda dapat dengan mudah memanggil skrip tersebut.

Sebagai contoh, jika Anda membuat skrip shell sederhana untuk dialek `sh` bernama `apti` dengan
```
sudo apt install "$1"
```
anda dapat memanggil skrip pada sistem apa pun yang kompatibel dengan `apti.sh <pkg>` dalam sesi terminal jika skrip tersebut diaktifkan.

## Beberapa jenis

Anda juga dapat mencentang beberapa kotak untuk tipe eksekusi skrip jika skrip tersebut harus digunakan dalam beberapa skenario.
