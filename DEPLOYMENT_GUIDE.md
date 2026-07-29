# Panduan Deployment Production (HobbyHub)

Panduan ini berisi langkah-langkah untuk membawa aplikasi HobbyHub Anda ke tahap **Production** (online 24/7) secara gratis (atau berbiaya sangat rendah) menggunakan Render, Neon, Supabase, dan Firebase.

## 1. Database (Neon PostgreSQL)
Neon.tech menyediakan PostgreSQL *Serverless* yang sangat cepat dan gratis.
1. Kunjungi [neon.tech](https://neon.tech/) dan buat akun.
2. Buat *Project* baru bernama "HobbyHub".
3. Setelah *database* terbuat, buka halaman **Dashboard** > **Connection Details**.
4. Salin string koneksi (Connection String) berupa URL:
   `postgresql://[user]:[password]@[host]/[dbname]?sslmode=require`
5. Anda akan memasukkan URL ini nanti ke Render sebagai `DATABASE_URL`.

## 2. Object Storage (Supabase Storage)
Supabase menyediakan *bucket* gratis hingga 1 GB (menggunakan infrastruktur S3).
1. Kunjungi [supabase.com](https://supabase.com/) dan buat *Project*.
2. Buka menu **Storage**, lalu klik **New Bucket**. Beri nama `hobbyhub-media`. Centang **"Public bucket"**.
3. Buka menu **Project Settings** (ikon gerigi) > **API**.
4. Cari `Project URL` (ini adalah `S3_ENDPOINT`, tambahkan HTTPS, misal: `https://[PROJECT-REF].supabase.co`).
5. Buka **Project Settings** > **Configuration** > **S3**.
   - Salin **Access Key** (`S3_ACCESS_KEY`).
   - Salin **Secret Key** (`S3_SECRET_KEY`).
   - Region (`S3_REGION`) sesuai dengan region yang tertera di sana.

## 3. Push Notification (Firebase Cloud Messaging)
1. Kunjungi [Firebase Console](https://console.firebase.google.com/).
2. Buat *Project* baru bernama "HobbyHub".
3. **Untuk Android:** Daftarkan aplikasi Android Anda (`com.hobbyhub`), *download* file `google-services.json`, lalu letakkan di folder `app/` di proyek Android.
4. **Untuk Backend:** Masuk ke **Project Settings** > **Service accounts** > **Generate new private key**.
5. Ganti nama file yang diunduh menjadi `firebase-service-account.json`. (Nanti file ini harus di-*upload* ke Root Directory di Render, atau disuntikkan via variabel).

## 4. Backend Deployment (Render.com)
Render memudahkan proses *deploy* Docker container.
1. Kunjungi [render.com](https://render.com/) dan buat akun.
2. Hubungkan akun GitHub Anda. Pastikan *source code* HobbyHub (atau minimal folder `hobbyhub-backend`) sudah di-*push* ke repositori GitHub.
3. Klik **New +** > **Web Service**.
4. Pilih repositori GitHub Anda.
5. Konfigurasi Web Service:
   - **Name**: `hobbyhub-api`
   - **Environment**: `Docker`
   - **Root Directory**: `hobbyhub-backend` (jika menggunakan repo gabungan).
   - **Instance Type**: Free atau Starter.
6. **Environment Variables**:
   Tambahkan variabel berikut agar dibaca oleh `application-prod.yml`:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DATABASE_URL` = `jdbc:postgresql://[HOST]/[DBNAME]?user=[USER]&password=[PASSWORD]&sslmode=require` (Ubah URL dari Neon ke format JDBC).
   - `JWT_SECRET` = (Buat string acak panjang minimal 64 karakter).
   - `S3_BUCKET_NAME` = `hobbyhub-media`
   - `S3_ACCESS_KEY` = (Dari Supabase S3)
   - `S3_SECRET_KEY` = (Dari Supabase S3)
   - `S3_REGION` = (Dari Supabase S3)
   - `S3_ENDPOINT` = (Project URL Supabase)
7. Klik **Create Web Service**. Tunggu proses *build* Docker selesai.
8. Salin URL publik dari Render (misal: `https://hobbyhub-api.onrender.com`).

## 5. Konfigurasi Android Studio
1. Buka file `D:\HobbyHub\hobbyhub-android\app\build.gradle.kts`.
2. Cari bagian `productFlavors` -> `prod`.
3. Ganti URL di sana dengan URL Render milik Anda:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://hobbyhub-api.onrender.com/\"")
   buildConfigField("String", "WS_BASE_URL", "\"wss://hobbyhub-api.onrender.com/\"")
   ```
4. Di panel kiri Android Studio, buka tab **Build Variants**, ubah *Active Build Variant* dari `devDebug` menjadi `prodDebug` atau `prodRelease`.
5. Klik **Sync Now** dan jalankan aplikasi di HP.

Kini aplikasi Anda sepenuhnya Online! 🚀
