# 2D Görüntüleme ve Kırpma Editörü

Bu proje Bilgisayar Grafikleri dersi için Java 17, Maven ve Processing kullanılarak hazırlanmıştır.

## Gereksinimler
- Java 17
- Apache Maven

## Projeyi Derleme ve Çalıştırma
```bash
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/odev3-kirpma-1.0-SNAPSHOT.jar
```

## Kontroller ve Etkileşim

**Genel Kontroller:**
- `1`, `2`, `3`: Görev/Mod değiştirme.
- `V`: Kırpma Penceresi Düzenleme modunu aç/kapat. Açıkken kırpma çerçevesinin kenarlarını farenizle sürükleyebilirsiniz.
- `Space`: Algoritmayı adım adım ilerlet.
- `R`: Orijinal şekli sıfırla.

**Görev 2 (Cohen-Sutherland Çizgi Kırpma):**
- `A` - `F`: Hazır test çizgilerini seç.
- **Fare Tıklaması:** Sol panele tıklayarak yeni bir çizginin başlangıç ve bitiş noktalarını belirleyebilirsiniz. Çizgi tamamlandığında anında kırpılır.

**Görev 3 (Sutherland-Hodgman Poligon Kırpma):**
- `K`: Konveks test poligonunu yükle.
- `U`: Konkav (U şeklinde) test poligonunu yükle.
- **Fare Tıklaması:** Sol panele tıklayarak kendi özel poligonunuzun köşe noktalarını ekleyebilirsiniz.
- `Enter`: Özel poligon çizimini bitir ve otomatik olarak kırp.
- `Backspace`: Özel poligon çizerken eklenen son noktayı geri al/sil.