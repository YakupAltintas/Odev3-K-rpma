# Bilgisayar Grafikleri Ödev 3: 2D Görüntüleme ve Kırpma Editörü

## 1. Giriş
Bu ödevde, bilgisayar grafiklerinin en temel konularından olan koordinat sistemleri dönüşümünü ve kırpma (clipping) algoritmalarını kodlayıp görselleştirdik. Amacımız sadece hazır fonksiyonları kullanmak değil, arkadaki matematiği (Y-Flip, bölge kodları, kenar kesişimleri) adım adım arayüze yansıtmaktı. Ortaya test verilerini canlı olarak deneyebildiğimiz mini bir editör çıktı.

## 2. Kullanılan Teknolojiler
- **Programlama Dili:** Java 17
- **Bağımlılık Yönetimi:** Maven
- **Grafik Kütüphanesi:** Processing 3 (Ekrana çizim yapmak, şekiller oluşturmak ve arayüz etkileşimi için)

## 3. Görev 1 - Koordinat Dönüşümü ve Y-Flip
İlk görevde matematiksel dünyadaki (Window) noktaları, ekrandaki piksellere (Viewport) dönüştürdük.

**Değerlerimiz:**
- Window: X = [-150, 150], Y = [-100, 100]
- Viewport: Başlangıç (50, 30), Bitiş (430, 290)
- Wx (Genişlik) = 300, Wy (Yükseklik) = 200
- Vx (Genişlik) = 380, Vy (Yükseklik) = 260
- Ölçekler: Sx = 380/300 = 1.266, Sy = 260/200 = 1.3

**Y-Flip Mantığı:**
Matematikte Y ekseni yukarı doğru artar, ama ekranda (Processing'de) aşağı doğru artar. Bu yüzden Y formülünde `(Yw_max - Y_dunya)` yaparak ekseni ters çevirmemiz gerekti.

**5 Noktanın El Hesabı:**
1. **(0, 0):** 
   - X = 50 + (0 - (-150)) * 1.266 = 50 + 190 = 240
   - Y = 30 + (100 - 0) * 1.3 = 30 + 130 = 160 -> Piksel: (240, 160)
2. **(150, 100):**
   - X = 50 + (150 + 150) * 1.266 = 430
   - Y = 30 + (100 - 100) * 1.3 = 30 -> Piksel: (430, 30) (Tam sağ üst)
3. **(-150, -100):**
   - X = 50 + (-150 + 150) * 1.266 = 50
   - Y = 30 + (100 - (-100)) * 1.3 = 290 -> Piksel: (50, 290) (Tam sol alt)
4. **(75, -50):**
   - X = 50 + (75 + 150) * 1.266 = 335
   - Y = 30 + (100 - (-50)) * 1.3 = 225 -> Piksel: (335, 225)
5. **(-30, 80):**
   - X = 50 + (-30 + 150) * 1.266 = 202
   - Y = 30 + (100 - 80) * 1.3 = 56 -> Piksel: (202, 56)

## 4. Görev 2 - Cohen-Sutherland Çizgi Kırpma
Bu algoritmada pencere dışına taşan çizgileri mantıksal BIT (bölge) kodlarıyla test ediyoruz. (Üst=1000, Alt=0100, Sağ=0010, Sol=0001). Pencere sınırı: (2,2) ile (8,6) arası.

**Test Edilen Çizgiler:**
- **A (2,2 -> 8,6):** P1=0000, P2=0000. OR=0000, AND=0000. Sonuç: **KABUL**. (Tam pencere içinde)
- **B (2,4 -> 8,4):** P1=0000, P2=0000. OR=0000, AND=0000. Sonuç: **KABUL**. (Yatay çizgi, içeride)
- **C (5,2 -> 5,10):** P1=0000, P2=1000. OR=1000, AND=0000. Sonuç: **KIRPMA**. Çizginin üst kısmı y=6 sınırından kesilerek atılır, kalan kısmı kabul edilir.
- **D (1,1 -> 1,5):** P1=0101, P2=0001. OR=0101, AND=0001. Sonuç: **RET**. AND sıfır değil, yani çizgi tamamen sol tarafta dışarıdadır.
- **E (0,0 -> 10,8):** P1=0101, P2=1010. OR=1111, AND=0000. Sonuç: **KIRPMA**. Çizgi pencereyi çapraz kesiyor. Birden fazla kenarda kesişim hesabı yapılarak ortadaki parça alınır.
- **F (5,5 -> 5,5):** P1=0000, P2=0000. OR=0000, AND=0000. Sonuç: **KABUL**. Tek nokta ve içeride.

## 5. Görev 3 - Sutherland-Hodgman Poligon Kırpma
Bu algoritmada poligonun noktalarını sırayla Sol, Sağ, Alt ve Üst kenarlardan geçiriyoruz. Her kenar geçişinde (İç->İç, İç->Dış, Dış->İç) kurallarına göre yeni bir nokta listesi çıkıyor.

- **Konveks Poligon Testi:** (-1,3), (5,-1), (11,3), (5,7) noktalarını (0,10) penceresine soktuk. Dışarı taşan sivri kısımlar pencere sınırlarında (x=0, x=10 vb.) kesilerek atıldı ve içerde daha küçük, düzgün bir çokgen oluştu.
- **Konkav (U Şekli) Testi:** (1,1)... gibi noktalardan oluşan U şeklini teste soktuk. Şekil zaten tamamen pencere içinde olduğu için algoritma tüm kenarlardan "İçeriden İçeriye" olarak döndü ve şekil hiçbir bozulmaya uğramadan orijinal haliyle kaldı.

## 6. Shoelace Alan Hesabı
Poligonların alan değişimini görmek için "Bağcık (Shoelace)" formülü kullandık. Formül köşelerin X ve Y değerlerini çapraz çarpıp çıkararak alanı bulur.
- Konveks poligon kırpıldığında alanı (48.0'dan 35.8 civarına) doğal olarak düştü.
- Konkav U poligon kırpılmadığı için alan (20.0) sabit kaldı.

## 7. Arayüz ve Kullanım
Projeyi daha eğlenceli hale getirmek için etkileşimli bir arayüz hazırladım:
- **1, 2, 3** tuşları ile görevler arasında geçiş yapılabiliyor.
- **Space** ile algoritmalar adım adım (loglar eşliğinde) izlenebiliyor.
- **Fare** ile sol panele tıklayıp kendi çizgilerimizi ve poligonlarımızı çizebiliyoruz.
- **V tuşu** ile Kırpma Penceresi Düzenleme modu açılıp, pencerenin kenarlarından tutup büyüklüğü değiştirildiğinde algoritma sonuçları anında yeniden hesaplanıyor.

## 8. Sonuç
Bu ödev sayesinde teorikte kağıt üzerinde yaptığımız matris/koordinat hesaplarının koda nasıl döküldüğünü gördüm. Özellikle özel durumlar (dik çizgi, sıfıra bölünme hatası) ve pencere dışına taşan karmaşık çokgenlerin kırpılma mantığı kafamda çok net oturdu.