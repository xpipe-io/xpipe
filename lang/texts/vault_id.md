# XPipe Git Vault

XPipe dapat menyinkronkan semua data koneksi Anda dengan repositori jarak jauh git Anda sendiri. Anda dapat melakukan sinkronisasi dengan repositori ini di semua contoh aplikasi XPipe dengan cara yang sama, setiap perubahan yang Anda lakukan dalam satu contoh akan tercermin dalam repositori.

Pertama-tama, Anda perlu membuat repositori jarak jauh dengan penyedia git favorit pilihan Anda. Repositori ini harus bersifat pribadi.
Anda kemudian dapat menyalin dan menempelkan URL ke dalam pengaturan repositori jarak jauh XPipe.

Anda juga harus memiliki klien `git` yang terinstal secara lokal di mesin lokal Anda. Anda dapat mencoba menjalankan `git` di terminal lokal untuk memeriksanya.
Jika Anda tidak memilikinya, Anda dapat mengunjungi [https://git-scm.com](https://git-scm.com/) untuk menginstal git.

## Mengautentikasi ke repositori jarak jauh

Ada beberapa cara untuk mengautentikasi. Sebagian besar repositori menggunakan HTTPS di mana Anda harus menentukan nama pengguna dan kata sandi.
Beberapa penyedia juga mendukung protokol SSH, yang juga didukung oleh XPipe.
Jika Anda menggunakan SSH untuk git, Anda mungkin tahu cara mengonfigurasinya, jadi bagian ini akan membahas HTTPS saja.

Anda perlu mengatur CLI git Anda untuk dapat mengautentikasi dengan repositori git jarak jauh melalui HTTPS. Ada beberapa cara untuk melakukannya.
Anda dapat memeriksa apakah hal itu sudah dilakukan dengan memulai ulang XPipe setelah repositori jarak jauh dikonfigurasi.
Jika ia meminta kredensial login Anda, Anda perlu mengaturnya.

Banyak alat khusus seperti ini [GitHub CLI](https://cli.github.com/) yang melakukan semuanya secara otomatis untuk Anda ketika diinstal.
Beberapa versi klien git yang lebih baru juga dapat mengautentikasi melalui layanan web khusus di mana Anda hanya perlu masuk ke akun Anda di peramban.

Ada juga cara manual untuk mengautentikasi melalui nama pengguna dan token.
Saat ini, sebagian besar penyedia layanan memerlukan token akses pribadi (PAT) untuk mengautentikasi dari baris perintah, bukan kata sandi tradisional.
Anda bisa menemukan halaman (PAT) yang umum di sini:
- **GitHub**: [Token akses pribadi (klasik)] (https://github.com/settings/tokens)
- **GitLab**: [Token akses pribadi] (https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Token akses pribadi] (https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Pengaturan -> Aplikasi -> bagian Kelola Token Akses`
Atur izin token untuk repositori ke Baca dan Tulis. Izin token lainnya dapat diatur sebagai Baca.
Meskipun klien git Anda meminta kata sandi, Anda harus memasukkan token Anda kecuali jika penyedia Anda masih menggunakan kata sandi.
- Sebagian besar penyedia layanan tidak lagi mendukung kata sandi.

Jika kamu tidak ingin memasukkan kredensial kamu setiap saat, kamu dapat menggunakan manajer kredensial git untuk itu.
Untuk informasi lebih lanjut, lihat misalnya:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Beberapa klien git modern juga menangani penyimpanan kredensial secara otomatis.

Jika semuanya berhasil, XPipe akan mendorong komit ke repositori jarak jauh Anda.

## Menambahkan kategori ke repositori

Secara default, tidak ada kategori koneksi yang diatur untuk disinkronkan sehingga Anda memiliki kontrol eksplisit pada koneksi apa yang akan dikomit.
Jadi pada awalnya, repositori jarak jauh Anda akan kosong.

Untuk memasukkan koneksi dari sebuah kategori ke dalam repositori git Anda,
kamu perlu mengklik ikon roda gigi (saat mengarahkan kursor ke kategori)
di tab `Connections` di bawah ikhtisar kategori di sisi kiri.
Kemudian klik `Tambahkan ke repositori git` untuk menyinkronkan kategori dan koneksi ke repositori git Anda.
Ini akan menambahkan semua koneksi yang dapat disinkronkan ke repositori git.

## Koneksi lokal tidak disinkronkan

Koneksi apa pun yang berada di bawah mesin lokal tidak dapat dibagikan karena mengacu pada koneksi dan data yang hanya tersedia di sistem lokal.

Koneksi tertentu yang didasarkan pada berkas lokal, misalnya konfigurasi SSH, dapat dibagikan melalui git jika data yang mendasarinya, dalam hal ini berkas, telah ditambahkan ke repositori git juga.

## Menambahkan berkas ke git

Ketika semuanya telah diatur, Anda memiliki opsi untuk menambahkan file tambahan seperti kunci SSH ke git.
Di samping setiap pilihan file terdapat tombol git yang akan menambahkan file tersebut ke repositori git.
File-file ini juga dienkripsi ketika di-push.
