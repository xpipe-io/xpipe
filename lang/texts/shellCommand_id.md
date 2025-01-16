## Sambungan cangkang khusus

Membuka shell menggunakan perintah khusus dengan menjalankan perintah yang diberikan pada sistem host yang dipilih. Shell ini dapat bersifat lokal atau jarak jauh.

Perhatikan bahwa fungsi ini mengharapkan shell yang digunakan adalah tipe standar seperti `cmd`, `bash`, dsb. Jika Anda ingin membuka jenis shell dan perintah lain di terminal, Anda dapat menggunakan jenis perintah terminal khusus. Menggunakan shell standar memungkinkan Anda untuk juga membuka koneksi ini di peramban file.

### Perintah interaktif

Proses shell mungkin akan terhenti atau hang jika ada prompt input yang tidak diharapkan
input yang tidak diharapkan, seperti prompt kata sandi. Oleh karena itu, Anda harus selalu memastikan bahwa tidak ada prompt input interaktif.

Sebagai contoh, perintah seperti `ssh user@host` akan bekerja dengan baik di sini selama tidak ada kata sandi yang diperlukan.

### Shell lokal khusus

Dalam banyak kasus, akan berguna untuk meluncurkan sebuah shell dengan opsi tertentu yang biasanya dinonaktifkan secara default untuk membuat beberapa skrip dan perintah bekerja dengan baik. Sebagai contoh:

-   [Ekspansi Tertunda di
    cmd] (https://ss64.com/nt/delayedexpansion.html)
-   [Eksekusi Powershell
    kebijakan] (https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Dan opsi peluncuran lain yang memungkinkan untuk shell pilihan Anda

Hal ini dapat dicapai dengan membuat perintah shell kustom dengan perintah berikut:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`