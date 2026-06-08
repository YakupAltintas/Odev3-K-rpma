package com.grafikler.editor;

import processing.core.PApplet;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainApp extends PApplet {

    private int mode = 1;
    private String modeName = "Koordinat Dönüşümü";

    // Ekran & Panel Düzeni Değerleri
    float panelW = 550, panelH = 480;
    float leftX = 60, rightX = 670, panelY = 100;

    // Görev 1 Değerleri
    float xwMin = -150, xwMax = 150, ywMin = -100, ywMax = 100;
    float xvMin = 50, xvMax = 430, yvMin = 30, yvMax = 290;
    float[][] testPoints = {{0, 0}, {150, 100}, {-150, -100}, {75, -50}, {-30, 80}};

    // Görev 2 Değerleri
    float csXMin = 2, csXMax = 8, csYMin = 2, csYMax = 6;
    float[][] testLines = {{2,2,8,6}, {2,4,8,4}, {5,2,5,10}, {1,1,1,5}, {0,0,10,8}, {5,5,5,5}};
    char[] lineNames = {'A', 'B', 'C', 'D', 'E', 'F'};
    int currentLineIdx = 0;
    float lineX1, lineY1, lineX2, lineY2, curX1, curY1, curX2, curY2;
    boolean csFinished = false, isAccepted = false;
    String g2LogP1 = "", g2LogP2 = "", g2LogOR = "", g2LogAND = "", g2LogResult = " Bekliyor...";
    List<String> g2Steps = new ArrayList<>();

    // Görev 3 Değerleri
    float polyClipXMin = 0, polyClipXMax = 10, polyClipYMin = 0, polyClipYMax = 10;
    List<float[]> originalPoly = new ArrayList<>();
    List<float[]> currentPoly = new ArrayList<>();
    int polyStep = 0;
    int polyType = 0; // 0: Konveks, 1: Konkav
    String g3EdgeName = "Başlangıç";

    public static void main(String[] args) {
        PApplet.main("com.grafikler.editor.MainApp");
    }

    @Override
    public void settings() {
        // Windows görev çubuğu altında kalmaması için yükseklik küçültüldü
        size(1280, 720);
        // Grafikleri yumuşatmak ve piksellenmeyi (tırtıklı görünümü) önlemek için yüksek seviye anti-aliasing (8x MSAA)
        smooth(8);
    }

    @Override
    public void setup() {
        surface.setTitle("2D Görüntüleme ve Kırpma Editörü");
        PFont myFont = createFont("Arial", 14, true);
        textFont(myFont);
        resetCS();
        resetPoly();
    }

    // --- YARDIMCI KOORDİNAT DÖNÜŞÜM FONKSİYONLARI (ÇİZİM İÇİN) ---
    private float mapWX(float x, float wMin, float wMax, float pMin, float pMax) {
        return pMin + (x - wMin) * (pMax - pMin) / (wMax - wMin);
    }
    private float mapWY(float y, float wMin, float wMax, float pMin, float pMax) {
        return pMax - (y - wMin) * (pMax - pMin) / (wMax - wMin); // Y-Flip uygulanmış hali
    }

    // --- GÖREV 1: KOORDİNAT DÖNÜŞÜMÜ (GERÇEK MATEMATİK) ---
    private float mapX(float xw) { return xvMin + (xw - xwMin) * ((xvMax - xvMin) / (xwMax - xwMin)); }
    private float mapY(float yw) { return yvMin + (ywMax - yw) * ((yvMax - yvMin) / (ywMax - ywMin)); }

    // --- GÖREV 2: COHEN-SUTHERLAND MANTIĞI ---
    private void resetCS() {
        lineX1 = testLines[currentLineIdx][0]; lineY1 = testLines[currentLineIdx][1];
        lineX2 = testLines[currentLineIdx][2]; lineY2 = testLines[currentLineIdx][3];
        curX1 = lineX1; curY1 = lineY1; curX2 = lineX2; curY2 = lineY2;
        csFinished = false; isAccepted = false;
        g2Steps.clear();
        g2LogResult = " İşlem Başlamadı";
        updateCSLogs();
    }

    private int getCode(float x, float y) {
        int code = 0;
        if (x < csXMin) code |= 1; else if (x > csXMax) code |= 2;
        if (y < csYMin) code |= 4; else if (y > csYMax) code |= 8;
        return code;
    }

    private String getCodeStr(int code) { return String.format("%4s", Integer.toBinaryString(code)).replace(' ', '0'); }

    private void updateCSLogs() {
        int c1 = getCode(curX1, curY1), c2 = getCode(curX2, curY2);
        g2LogP1 = getCodeStr(c1); g2LogP2 = getCodeStr(c2);
        g2LogOR = getCodeStr(c1 | c2); g2LogAND = getCodeStr(c1 & c2);
    }

    private void doCSStep() {
        if (csFinished) return;
        int c1 = getCode(curX1, curY1), c2 = getCode(curX2, curY2);
        if ((c1 | c2) == 0) {
            csFinished = true; isAccepted = true; g2LogResult = " KABUL EDİLDİ (Tamamen İçeride)"; 
            g2Steps.add("Çizgi tamamen pencere içinde, kabul edildi."); return;
        } else if ((c1 & c2) != 0) {
            csFinished = true; isAccepted = false; g2LogResult = " REDDEDİLDİ (Tamamen Dışarıda)"; 
            g2Steps.add("Çizgi tamamen pencere dışında (AND != 0), reddedildi."); return;
        }
        int outCode = (c1 != 0) ? c1 : c2;
        float ix = 0, iy = 0;
        float dx = (curX2 - curX1) == 0 ? 0.0001f : (curX2 - curX1);
        float dy = (curY2 - curY1) == 0 ? 0.0001f : (curY2 - curY1);
        String eName = "";
        if ((outCode & 8) != 0) { ix = curX1 + dx * (csYMax - curY1) / dy; iy = csYMax; eName="Üst"; }
        else if ((outCode & 4) != 0) { ix = curX1 + dx * (csYMin - curY1) / dy; iy = csYMin; eName="Alt"; }
        else if ((outCode & 2) != 0) { iy = curY1 + dy * (csXMax - curX1) / dx; ix = csXMax; eName="Sağ"; }
        else if ((outCode & 1) != 0) { iy = curY1 + dy * (csXMin - curX1) / dx; ix = csXMin; eName="Sol"; }
        
        g2Steps.add(eName + " kenarında kesişim bulundu: (" + String.format(Locale.US, "%.1f, %.1f", ix, iy) + ")");
        if (outCode == c1) { curX1 = ix; curY1 = iy; } else { curX2 = ix; curY2 = iy; }
        updateCSLogs();
    }

    // --- GÖREV 3: SUTHERLAND-HODGMAN MANTIĞI VE ALAN HESABI ---
    private float calculateArea(List<float[]> poly) {
        if (poly == null || poly.size() < 3) return 0;
        float sum1 = 0, sum2 = 0;
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            sum1 += poly.get(i)[0] * poly.get((i + 1) % n)[1];
            sum2 += poly.get(i)[1] * poly.get((i + 1) % n)[0];
        }
        return Math.abs(sum1 - sum2) / 2.0f;
    }

    private void resetPoly() {
        originalPoly.clear();
        if (polyType == 0) {
            originalPoly.add(new float[]{-1, 3}); originalPoly.add(new float[]{5, -1});
            originalPoly.add(new float[]{11, 3}); originalPoly.add(new float[]{5, 7});
        } else {
            originalPoly.add(new float[]{1, 1}); originalPoly.add(new float[]{9, 1});
            originalPoly.add(new float[]{9, 4}); originalPoly.add(new float[]{6, 4});
            originalPoly.add(new float[]{6, 2}); originalPoly.add(new float[]{4, 2});
            originalPoly.add(new float[]{4, 4}); originalPoly.add(new float[]{1, 4});
        }
        currentPoly = new ArrayList<>(originalPoly);
        polyStep = 0;
        g3EdgeName = "Başlangıç Durumu";
    }

    private boolean isInside(float[] p, int edge) {
        if (edge == 0) return p[0] >= polyClipXMin;      
        if (edge == 1) return p[0] <= polyClipXMax;     
        if (edge == 2) return p[1] >= polyClipYMin;      
        if (edge == 3) return p[1] <= polyClipYMax;     
        return false;
    }

    private float[] getIntersect(float[] p1, float[] p2, int edge) {
        float x1 = p1[0], y1 = p1[1], x2 = p2[0], y2 = p2[1];
        float ix = 0, iy = 0;
        float dx = x2 - x1, dy = y2 - y1;
        if (dx == 0) dx = 0.0001f; if (dy == 0) dy = 0.0001f;
        if (edge == 0)      { ix = polyClipXMin; iy = y1 + dy * (polyClipXMin - x1) / dx; }
        else if (edge == 1) { ix = polyClipXMax; iy = y1 + dy * (polyClipXMax - x1) / dx; }
        else if (edge == 2) { iy = polyClipYMin; ix = x1 + dx * (polyClipYMin - y1) / dy; }
        else if (edge == 3) { iy = polyClipYMax; ix = x1 + dx * (polyClipYMax - y1) / dy; }
        return new float[]{ix, iy};
    }

    private void doPolyStep() {
        if (polyStep > 3) return;
        List<float[]> nextPoly = new ArrayList<>();
        int edge = polyStep;
        g3EdgeName = (edge==0)?"Sol Kenar (x=0)" : (edge==1)?"Sağ Kenar (x=10)" : (edge==2)?"Alt Kenar (y=0)" : "Üst Kenar (y=10)";
        
        if (currentPoly.size() > 0) {
            float[] S = currentPoly.get(currentPoly.size() - 1);
            for (float[] E : currentPoly) {
                boolean sIn = isInside(S, edge), eIn = isInside(E, edge);
                if (sIn && eIn) { nextPoly.add(E); }
                else if (sIn && !eIn) { nextPoly.add(getIntersect(S, E, edge)); }
                else if (!sIn && !eIn) { }
                else if (!sIn && eIn) { nextPoly.add(getIntersect(S, E, edge)); nextPoly.add(E); }
                S = E;
            }
        }
        currentPoly = nextPoly;
        polyStep++;
        if(polyStep > 3) g3EdgeName = "Kırpma Tamamlandı";
    }

    // --- ÇİZİM VE ARAYÜZ (ANA DÖNGÜ) ---
    @Override
    public void draw() {
        background(30);
        drawHeader();
        if (mode == 1) drawMode1();
        else if (mode == 2) drawMode2();
        else if (mode == 3) drawMode3();
        drawFooter();
    }

    private void drawHeader() {
        fill(255); textAlign(CENTER, TOP); textSize(24);
        text("2D Görüntüleme ve Kırpma Editörü", width / 2f, 20);
        textSize(16); fill(200); text("Mevcut Mod: " + modeName, width / 2f, 50);
        stroke(100); line(50, 80, width - 50, 80);
    }

    private void drawFooter() {
        stroke(100); line(50, height - 50, width - 50, height - 50);
        fill(200); textAlign(CENTER, BOTTOM); textSize(14);
        String info = "[1] Görev 1 | [2] Görev 2 | [3] Görev 3  ---  [A-F / K-U] Test Seç  ---  [Space] Adım İlerle  ---  [R] Sıfırla";
        text(info, width / 2f, height - 15);
    }

    // --- GÖREV 1 ARAYÜZÜ ---
    private void drawMode1() {
        // Sol Panel: Dünya
        fill(40); stroke(100); rect(leftX, panelY, panelW, panelH, 5);
        fill(255); textAlign(CENTER, TOP); textSize(18);
        text("Dünya Koordinatları (Window)", leftX + panelW / 2f, panelY - 25);
        
        stroke(80); 
        float originX = mapWX(0, xwMin, xwMax, leftX, leftX+panelW);
        float originY = mapWY(0, ywMin, ywMax, panelY, panelY+panelH);
        line(leftX, originY, leftX+panelW, originY); // X ekseni
        line(originX, panelY, originX, panelY+panelH); // Y ekseni

        // Sağ Panel: Viewport
        fill(40); stroke(100); rect(rightX, panelY, panelW, panelH, 5);
        fill(255); textAlign(CENTER, TOP);
        text("Viewport / Piksel Karşılığı", rightX + panelW / 2f, panelY - 25);
        
        // Viewport Kutusunu sağ panelin içine göreceli çiz
        float vpScreenX = rightX + xvMin;
        float vpScreenY = panelY + yvMin;
        float vpW = xvMax - xvMin;
        float vpH = yvMax - yvMin;
        stroke(0, 150, 255); noFill(); strokeWeight(2);
        rect(vpScreenX, vpScreenY, vpW, vpH);
        strokeWeight(1);

        // Noktaları Çiz
        for (float[] p : testPoints) {
            // Sol panel çizimi
            float pxW = mapWX(p[0], xwMin, xwMax, leftX, leftX+panelW);
            float pyW = mapWY(p[1], ywMin, ywMax, panelY, panelY+panelH);
            fill(255, 255, 0); noStroke(); ellipse(pxW, pyW, 8, 8);
            fill(220); textAlign(LEFT, BOTTOM); textSize(12);
            text(String.format("W(%.0f, %.0f)", p[0], p[1]), pxW + 5, pyW - 5);

            // Sağ panel çizimi
            float pxV = rightX + mapX(p[0]);
            float pyV = panelY + mapY(p[1]);
            fill(0, 255, 0); ellipse(pxV, pyV, 8, 8);
            fill(220); text(String.format("V(%.0f, %.0f)", mapX(p[0]), mapY(p[1])), pxV + 5, pyV - 5);
        }

        // Alt Formül
        fill(255, 255, 0); textAlign(CENTER, TOP); textSize(14);
        text("Hesaplama: X_v = 50 + (X_w + 150) * 1.266   |   Y_v = 30 + (100 - Y_w) * 1.3 (Y-Flip)", width/2f, panelY + panelH + 15);
    }

    // --- GÖREV 2 ARAYÜZÜ ---
    private void drawMode2() {
        fill(40); stroke(100); rect(leftX, panelY, panelW, panelH, 5); rect(rightX, panelY, panelW, panelH, 5);
        fill(255); textAlign(CENTER, TOP); textSize(18);
        text("Orijinal Çizgi (Çizgi " + lineNames[currentLineIdx] + ")", leftX + panelW / 2f, panelY - 25);
        text("Kırpılmış Sonuç ve Loglar", rightX + panelW / 2f, panelY - 25);

        // Sol Panel Izgara ve Pencere
        stroke(60); 
        for(int i=0; i<=10; i++) {
            line(mapWX(i, 0, 10, leftX, leftX+panelW), panelY, mapWX(i, 0, 10, leftX, leftX+panelW), panelY+panelH);
            line(leftX, mapWY(i, 0, 10, panelY, panelY+panelH), leftX+panelW, mapWY(i, 0, 10, panelY, panelY+panelH));
        }
        stroke(0, 150, 255); noFill(); strokeWeight(3);
        rectMode(CORNERS);
        rect(mapWX(csXMin, 0, 10, leftX, leftX+panelW), mapWY(csYMax, 0, 10, panelY, panelY+panelH),
             mapWX(csXMax, 0, 10, leftX, leftX+panelW), mapWY(csYMin, 0, 10, panelY, panelY+panelH));
        rectMode(CORNER); strokeWeight(1);

        // Orijinal Çizgi
        stroke(150); strokeWeight(3);
        line(mapWX(lineX1, 0, 10, leftX, leftX+panelW), mapWY(lineY1, 0, 10, panelY, panelY+panelH),
             mapWX(lineX2, 0, 10, leftX, leftX+panelW), mapWY(lineY2, 0, 10, panelY, panelY+panelH));
        strokeWeight(1);

        // Sağ Panel Üst Yarı: Kırpılmış Görsel
        float rpDrawH = 260;
        stroke(60); 
        for(int i=0; i<=10; i++) {
            line(mapWX(i, 0, 10, rightX, rightX+panelW), panelY, mapWX(i, 0, 10, rightX, rightX+panelW), panelY+rpDrawH);
            line(rightX, mapWY(i, 0, 10, panelY, panelY+rpDrawH), rightX+panelW, mapWY(i, 0, 10, panelY, panelY+rpDrawH));
        }
        stroke(0, 150, 255); noFill(); strokeWeight(3); rectMode(CORNERS);
        rect(mapWX(csXMin, 0, 10, rightX, rightX+panelW), mapWY(csYMax, 0, 10, panelY, panelY+rpDrawH),
             mapWX(csXMax, 0, 10, rightX, rightX+panelW), mapWY(csYMin, 0, 10, panelY, panelY+rpDrawH));
        rectMode(CORNER); strokeWeight(1);

        if (!csFinished) {
            stroke(255, 165, 0); strokeWeight(3);
            line(mapWX(curX1, 0, 10, rightX, rightX+panelW), mapWY(curY1, 0, 10, panelY, panelY+rpDrawH),
                 mapWX(curX2, 0, 10, rightX, rightX+panelW), mapWY(curY2, 0, 10, panelY, panelY+rpDrawH));
        } else if (isAccepted) {
            stroke(0, 255, 0); strokeWeight(4);
            line(mapWX(curX1, 0, 10, rightX, rightX+panelW), mapWY(curY1, 0, 10, panelY, panelY+rpDrawH),
                 mapWX(curX2, 0, 10, rightX, rightX+panelW), mapWY(curY2, 0, 10, panelY, panelY+rpDrawH));
        }
        fill(255, 255, 0); noStroke();
        ellipse(mapWX(curX1, 0, 10, rightX, rightX+panelW), mapWY(curY1, 0, 10, panelY, panelY+rpDrawH), 8, 8);
        ellipse(mapWX(curX2, 0, 10, rightX, rightX+panelW), mapWY(curY2, 0, 10, panelY, panelY+rpDrawH), 8, 8);
        if(csFinished && !isAccepted) {
            fill(255, 0, 0); textAlign(CENTER, CENTER); textSize(24);
            text("REJECTED", rightX + panelW/2f, panelY + rpDrawH/2f);
        }

        // Sağ Panel Alt Yarı: Loglar
        stroke(100); line(rightX, panelY + rpDrawH, rightX + panelW, panelY + rpDrawH);
        fill(220); textAlign(LEFT, TOP); textSize(14);
        float logY = panelY + rpDrawH + 15;
        text("P1 Kodu: " + g2LogP1 + "   |   P2 Kodu: " + g2LogP2, rightX + 20, logY); logY += 25;
        text("OR Sonucu: " + g2LogOR + "   |   AND Sonucu: " + g2LogAND, rightX + 20, logY); logY += 25;
        fill(csFinished ? (isAccepted ? color(0,255,0) : color(255,0,0)) : color(255,255,0));
        text("Durum:" + g2LogResult, rightX + 20, logY); logY += 30;
        fill(200); textSize(12);
        for(String s : g2Steps) { text("- " + s, rightX + 20, logY); logY += 20; }
    }

    // --- GÖREV 3 ARAYÜZÜ ---
    private void drawMode3() {
        fill(40); stroke(100); rect(leftX, panelY, panelW, panelH, 5); rect(rightX, panelY, panelW, panelH, 5);
        
        float origArea = calculateArea(originalPoly), currArea = calculateArea(currentPoly);
        fill(255); textAlign(CENTER, TOP); textSize(18);
        text("Orijinal Poligon", leftX + panelW / 2f, panelY - 25);
        text("Kırpılmış Poligon (" + g3EdgeName + ")", rightX + panelW / 2f, panelY - 25);

        // Sol Çizim
        drawPolyGrid(leftX, panelY, panelW, panelH);
        stroke(150); strokeWeight(2); fill(150, 150, 150, 100);
        beginShape(); for(float[] p : originalPoly) vertex(mapWX(p[0], -4, 14, leftX, leftX+panelW), mapWY(p[1], -4, 14, panelY, panelY+panelH)); endShape(CLOSE);

        // Sağ Çizim
        drawPolyGrid(rightX, panelY, panelW, panelH);
        stroke(0, 255, 0); strokeWeight(3); fill(0, 255, 0, 100);
        beginShape(); for(float[] p : currentPoly) vertex(mapWX(p[0], -4, 14, rightX, rightX+panelW), mapWY(p[1], -4, 14, panelY, panelY+panelH)); endShape(CLOSE);
        fill(255, 255, 0); noStroke();
        for(float[] p : currentPoly) ellipse(mapWX(p[0], -4, 14, rightX, rightX+panelW), mapWY(p[1], -4, 14, panelY, panelY+panelH), 8, 8);

        // Alt Kısım Nokta Listesi ve Alan
        fill(255, 255, 0); textAlign(CENTER, TOP); textSize(14);
        text("Orijinal Alan: " + String.format("%.1f", origArea), leftX + panelW/2f, panelY + panelH + 10);
        text("Kırpılmış Alan: " + String.format("%.1f", currArea), rightX + panelW/2f, panelY + panelH + 10);
        
        fill(200); textSize(12);
        StringBuilder sb = new StringBuilder("Kırpılmış Noktalar: ");
        for(float[] p : currentPoly) sb.append(String.format(Locale.US, "(%.1f, %.1f)  ", p[0], p[1]));
        text(sb.toString(), width/2f, panelY + panelH + 30);
    }

    private void drawPolyGrid(float x, float y, float w, float h) {
        stroke(60); strokeWeight(1);
        for(int i=-4; i<=14; i+=2) {
            line(mapWX(i, -4, 14, x, x+w), y, mapWX(i, -4, 14, x, x+w), y+h);
            line(x, mapWY(i, -4, 14, y, y+h), x+w, mapWY(i, -4, 14, y, y+h));
        }
        stroke(0, 150, 255); noFill(); strokeWeight(3); rectMode(CORNERS);
        rect(mapWX(0, -4, 14, x, x+w), mapWY(10, -4, 14, y, y+h), mapWX(10, -4, 14, x, x+w), mapWY(0, -4, 14, y, y+h));
        rectMode(CORNER); strokeWeight(1);
    }

    // --- KLAVYE ETKİLEŞİMLERİ ---
    @Override
    public void keyPressed() {
        char k = Character.toUpperCase(key);
        if (k == '1') { mode = 1; modeName = "Koordinat Dönüşümü"; }
        else if (k == '2') { mode = 2; modeName = "Cohen-Sutherland Çizgi Kırpma"; resetCS(); }
        else if (k == '3') { mode = 3; modeName = "Sutherland-Hodgman Poligon Kırpma"; resetPoly(); }
        else if (k == ' ') {
            if (mode == 2) doCSStep(); else if (mode == 3) doPolyStep();
        }
        else if (k == 'R') {
            if (mode == 2) resetCS(); else if (mode == 3) resetPoly();
        }
        else if (mode == 2 && k >= 'A' && k <= 'F') {
            currentLineIdx = k - 'A'; resetCS();
        }
        else if (mode == 3) {
            if (k == 'K') { polyType = 0; resetPoly(); }
            else if (k == 'U') { polyType = 1; resetPoly(); }
        }
    }
}
