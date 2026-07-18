# Optimum — Kişisel Üretkenlik ve Zaman Takip Asistanı

Optimum; modern, local-first (öncelikli yerel veri) mimariye sahip, dairesel Pomodoro zamanlayıcı ile entegre edilmiş şık ve gelişmiş bir kişisel zaman takip web/mobil uygulamasıdır.

Kullanıcıların gün içindeki zaman bloklarını verimli/verimsiz kategorilerle planlamasına, istatistiklerini izlemesine, hedefler belirlemesine ve başarı rozetleri kazanmasına olanak tanır.

---

## ✨ Öne Çıkan Özellikler

### 1. Zaman Paneli (Dashboard Grid)
- **Hassas Zaman Girişi:** 15, 30, 60 veya 120 dakikalık esnek zaman aralıklarıyla günün her dilimine aktivite kaydı ekleme.
- **Kategori Kodlama:** Özelleştirilmiş renklerle görsel zaman haritası.
- **İnteraktif Kontroller:** Zoom-in/out, pinched-zoom (mobil), tam ekran modu ve PDF/CSV dışa aktarım.
- **Akıllı İstatistikler:** Günlük verimli saat hedefleri, doldurulmamış zaman slotu uyarıları ve dinamik verimlilik zinciri (streak) hesaplamaları.

### 2. Pomodoro Zamanlayıcı
- **SVG Halka Görselleştirme:** Kalan süreyi akıcı animasyonlarla gösteren modern arayüz.
- **Çoklu Faz Desteği:** Çalışma, Kısa Mola ve Uzun Mola geçişleri.
- **Gelişmiş Sesler:** 4 farklı yüksek kaliteli bildirim sesi seçeneği ve sesli/bildirimli hatırlatıcılar.
- **Hafıza Dayanıklılığı:** Sayfa yenilendiğinde veya kapatıldığında kaldığı yerden devam eden oturum kontrolü.

### 3. Gelişmiş Analizler
- **Dinamik Zaman Ölçeği:** 1-60 gün arası kaydırıcı (slider) ile anında güncellenen grafikler.
- **Çok Boyutlu Grafikler:** Üretkenlik Trend Çizgisi, Kategori Dağılım Donut Grafiği, Günlük Üretkenlik Yığılmış Bar Grafiği.
- **Sıralamalar:** En çok vakit harcanan Top 5 aktivite ve filtrelenebilir geçmiş kayıt listesi (Daha Fazla Yükle sayfalama desteğiyle).

### 4. Ayarlar & Veri Yönetimi
- **Kategori & Aktivite Yönetimi (CRUD):** Tamamen özelleştirilebilir kategoriler ve bunlara bağlı alt aktiviteler.
- **Yedekleme & Sıfırlama:** Tüm yerel verileri JSON olarak indirme, geri yükleme ve hesabı kalıcı silme.
- **Geliştirici Dostu:** Geliştirme modunda (Local Dev) hızlı testler için **Demo Veri Yükleme** aracı.

---

## 🛠️ Teknoloji Yığınımız

- **Çekirdek:** [React 18](https://reactjs.org/) + [TypeScript](https://www.typescriptlang.org/)
- **Derleyici & Sunucu:** [Vite 8](https://vitejs.dev/)
- **Stil & Tasarım:** Vanilla CSS (Glassmorphism, custom responsive layouts, dark theme)
- **Grafikler & İkonlar:** SVG + [Lucide React](https://lucide.dev/)
- **Arka Plan & Senkronizasyon:** [Supabase](https://supabase.com/) (Çevrimdışı yerel depolama ile otomatik eşitleme)
- **Mobil Paketleme:** [Capacitor CLI](https://capacitorjs.com/) (Android / iOS yerel uygulama dönüşümü için hazır altyapı)

---

## 🚀 Yerelde Çalıştırma Adımları

### 1. Depoyu Klonlayın ve Bağımlılıkları Kurun
```bash
git clone https://github.com/sata2500/optimum.git
cd optimum
npm install
```

### 2. Çevre Değişkenlerini Yapılandırın (Opsiyonel)
Kök dizinde bir `.env` dosyası oluşturun ve Supabase bilgilerinizi ekleyin (eklenmezse uygulama yerel veritabanında çalışmaya devam edecektir):
```env
VITE_SUPABASE_URL=https://your-project-id.supabase.co
VITE_SUPABASE_ANON_KEY=your-supabase-anon-key
```

### 3. Geliştirme Sunucusunu Başlatın
```bash
npm run dev
```
Uygulama varsayılan olarak `http://localhost:5173` adresinde çalışacaktır.

### 4. Üretim Sürümü İçin Derleyin
```bash
npm run build
```
Derlenmiş statik dosyalar `/dist` klasörü altında oluşturulacaktır.

---

## 📱 Mobil (Android) Paketleme Süreci

Bu proje Capacitor entegrasyonu ile Android yerel uygulaması olarak paketlenmeye hazır durumdadır:

1. **Statik Derleme Alın:**
   ```bash
   npm run build
   ```
2. **Android Platformunu Ekleyin (İlk kez çalıştırılıyorsa):**
   ```bash
   npx cap add android
   ```
3. **Statik Dosyaları Eşitleyin:**
   ```bash
   npx cap sync
   ```
4. **Android Studio'yu Başlatın:**
   ```bash
   npx cap open android
   ```
   *Açılan Android Studio ekranında doğrudan APK veya AAB derlemesi alabilirsiniz.*
