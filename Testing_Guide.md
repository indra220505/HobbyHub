# Panduan Pengujian Voice Chat di HP Fisik (WebRTC)

## 1. Mengetahui IP Lokal Komputer (Backend)
Backend Spring Boot sekarang berjalan di `0.0.0.0` yang artinya bisa diakses dari perangkat manapun di jaringan Wi-Fi yang sama.
1. Buka **Command Prompt (CMD)** atau PowerShell di Windows.
2. Ketik `ipconfig` dan tekan Enter.
3. Cari bagian **Wireless LAN adapter Wi-Fi** (atau Ethernet jika pakai kabel).
4. Catat alamat IP pada baris **IPv4 Address** (misalnya `192.168.1.5` atau `192.168.100.12`).

## 2. Mengkonfigurasi Aplikasi Android
1. Buka file `app/build.gradle.kts` di Android Studio.
2. Di blok `buildTypes -> debug`, ubah alamat IP `10.0.2.2` menjadi IP lokal komputer Anda.
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.5:8080/\"")
   buildConfigField("String", "WS_BASE_URL", "\"ws://192.168.1.5:8080/\"")
   ```
3. Klik tombol **Sync Now** (ikon gajah di kanan atas) di Android Studio.

## 3. Menjalankan Backend
1. Pastikan Anda sudah menjalankan backend via Android Studio/IntelliJ atau jalankan command gradle: `.\gradlew bootRun` di dalam folder `hobbyhub-backend`.
2. Pastikan tidak ada error di console dan backend menyatakan "Started HobbyHubApplication... on port 8080".

## 4. Pengujian Dua Perangkat
1. **Instalasi:** Hubungkan HP pertama ke laptop via USB/Wireless Debugging dan klik tombol **Run** (Play) di Android Studio. Setelah berhasil terinstall, cabut kabel/hentikan Run.
2. Hubungkan HP kedua dan lakukan hal yang sama (klik **Run**).
3. **Pastikan kedua HP terhubung ke WiFi yang sama** dengan laptop Anda.
4. Buka aplikasi di kedua HP, lalu Login atau Register dengan 2 akun yang **berbeda**.
5. Karena saat ini API komunitas belum sepenuhnya selesai (masih proses pivot), Anda mungkin harus menavigasi ke Voice Room melalui menu Explore.
6. **Bergabung ke Voice Room:** Saat kedua HP masuk ke room Voice yang sama (misal "General Voice"):
   - Anda akan melihat *request permission* untuk Mikrofon (Allow/Izinkan).
   - Di console backend, akan terlihat log: `New WebSocket connection established...` dan `User XYZ joined room General Voice`.
   - Di kedua layar HP, jumlah peserta akan menjadi 2.
   - Bicaralah ke mikrofon HP 1, Anda akan mendengar suara Anda di *speaker* HP 2 (dengan latency rendah melalui koneksi P2P WebRTC Google STUN).

## Troubleshooting
- **Error CLEARTEXT**: Sudah diizinkan melalui `network_security_config.xml`.
- **Tidak Konek/Connection Refused**: Pastikan IP di `build.gradle.kts` benar, **Windows Firewall** tidak memblokir port 8080. Jika perlu, matikan Windows Firewall sementara untuk mengetes jaringan lokal (Private Network).
- **Suara Tidak Terdengar**: Coba periksa volume media di HP, tekan tombol "Mute" dan "Unmute" di layar aplikasi, pastikan izin mikrofon tidak terblokir.
