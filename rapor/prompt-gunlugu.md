# Yapay Zeka Prompt Günlüğü

Ödev sürecinde bazı teknik detaylarda ve kod hamallığı gerektiren yerlerde yapay zekadan destek aldım. Sürecin özeti şu şekilde:

### Proje iskeleti için destek
Projeye başlarken Java ve Processing'i Maven ile nasıl bağlayacağımı tam oturtamamıştım. Asistandan pom.xml ayarlarını yapıp boş bir pencere açmasını istedim. Böylece derleme hatalarıyla uğraşmadan direkt asıl ödeve odaklanabildim.

### Arayüz tasarımı için destek
Ödevde ekranı ikiye bölüp sol tarafa orijinali, sağ tarafa kırpılmış halini koymam gerekiyordu. Arayüzün düzgün görünmesi ve panel ayarları için yardım aldım. Temiz bir ekran olunca kodları ve algoritmaları test etmesi çok daha kolay oldu.

### Koordinat dönüşümü için destek
Processing'de Y ekseninin ters (aşağı doğru artıyor) olması kafamı bayağı karıştırdı. Y-Flip mantığını koda dökerken formüllerdeki oran (scale) hesapları için AI'dan destek istedim. Ekrana 5 test noktası basıp sonuçların doğruluğunu el hesabıyla teyit ettik.

### Cohen-Sutherland algoritması için destek
Bölge kodlarını (0001, 1000 gibi) koda döküp AND/OR mantığıyla sınır testi yapmak biraz uğraştırıcıydı. Algoritmanın adım adım çalışmasını sağlamak için döngü yapısını beraber kurduk. Space tuşuna basarak her adımı ekranda görebilmek işimi çok kolaylaştırdı.

### Edge case testleri için destek
Dikey veya yatay çizgilerde sıfıra bölünme hatası yüzünden program çöküp duruyordu. Bu tarz uç durumları ve tam köşeden geçen çizgileri patlamadan atlatmak için koda ufak toleranslar ekledik. Sonrasında mouse ile serbest çizgi çizme özelliği ekleterek kendi testlerimi de yapabildim.

### Sutherland-Hodgman algoritması için destek
Ödevin en zorlandığım kısmı poligon kırpmaydı çünkü şekli 4 kenardan sırayla geçirip yeni listeler oluşturmak gerekiyordu. Algoritmanın "içeriden dışarıya" geçişlerdeki kesişim noktası hesaplamalarını toparlamak için destek aldım. Konveks ve U şeklindeki konkav testleri deneyip doğru çalıştığını beraber gördük.

### Alan hesabı ve rapor düzenleme için destek
Kırpma sonrası poligonun alan değişimini ekranda göstermek için Shoelace (Bağcık) formülünü koda entegre ettik. En son aşamada da yaptığımız tüm el hesaplarını ve algoritma sonuçlarını toparlaması için yardım istedim. Böylece ödevin teorik analiz raporunu daha hızlı bitirebildim.