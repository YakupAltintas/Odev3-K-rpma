package com.grafikler.editor;

import processing.core.PApplet;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainApp extends PApplet {

    int mode = 1;
    boolean vMode = false; 
    int hoverState = -1; 
    int dragState = -1;
    String perfMsg = "";

    float pW = 500, pH = 480;
    float lX = 50, rX = 600, pY = 90;

    // Görev 1
    float xwMin = -150, xwMax = 150, ywMin = -100, ywMax = 100;
    float xvMin = 50, xvMax = 430, yvMin = 30, yvMax = 290;
    float[][] testPoints = {{0,0}, {150,100}, {-150,-100}, {75,-50}, {-30,80}};

    // Görev 2
    float w2XMin = -2, w2XMax = 12, w2YMin = -2, w2YMax = 12;
    float csXMin = 2, csXMax = 8, csYMin = 2, csYMax = 6;
    List<LineObj> lines = new ArrayList<>();
    boolean isDrawingLine = false;
    float tempX, tempY;
    int currentLineIdx = 0;

    // Görev 3
    float w3XMin = -4, w3XMax = 14, w3YMin = -4, w3YMax = 14;
    float shXMin = 0, shXMax = 10, shYMin = 0, shYMax = 10;
    List<PolyObj> polys = new ArrayList<>();
    boolean isDrawingPoly = false;
    List<float[]> customPoly = new ArrayList<>();
    int polyType = 0; // 0: Konveks, 1: Konkav, 2: Özel

    public static void main(String[] args) { PApplet.main("com.grafikler.editor.MainApp"); }

    @Override
    public void settings() {
        size(1280, 720);
        smooth(8);
    }

    @Override
    public void setup() {
        surface.setTitle("2D Görüntüleme ve Kırpma Editörü");
        textFont(createFont("Arial", 14, true));
        addLine(2,2,8,6);
        addPoly(0);
    }

    float w2sX(float wx, float vX, float vW, float wMin, float wMax) { return vX + (wx - wMin) * (vW / (wMax - wMin)); }
    float w2sY(float wy, float vY, float vH, float wMin, float wMax) { return vY + vH - (wy - wMin) * (vH / (wMax - wMin)); }
    float s2wX(float sx, float vX, float vW, float wMin, float wMax) { return wMin + (sx - vX) * ((wMax - wMin) / vW); }
    float s2wY(float sy, float vY, float vH, float wMin, float wMax) { return wMin + (vY + vH - sy) * ((wMax - wMin) / vH); }

    float getCurWMinX() { return mode==2 ? w2XMin : w3XMin; }
    float getCurWMaxX() { return mode==2 ? w2XMax : w3XMax; }
    float getCurWMinY() { return mode==2 ? w2YMin : w3YMin; }
    float getCurWMaxY() { return mode==2 ? w2YMax : w3YMax; }
    float getCurCMinX() { return mode==2 ? csXMin : shXMin; }
    float getCurCMaxX() { return mode==2 ? csXMax : shXMax; }
    float getCurCMinY() { return mode==2 ? csYMin : shYMin; }
    float getCurCMaxY() { return mode==2 ? csYMax : shYMax; }

    class LineObj {
        float x1, y1, x2, y2, cx1, cy1, cx2, cy2;
        boolean done, accepted;
        int origC1, origC2, c1, c2, animState;
        List<String> animSteps = new ArrayList<>();
        List<String> intersectLogs = new ArrayList<>();

        LineObj(float x1, float y1, float x2, float y2, boolean isInstant) { 
            this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2; 
            init(); if(isInstant) calcInstant();
        }
        void init() {
            cx1=x1; cy1=y1; cx2=x2; cy2=y2; 
            origC1=getCode(x1,y1); origC2=getCode(x2,y2);
            c1=origC1; c2=origC2; 
            done=false; accepted=false; animState=0; 
            animSteps.clear(); intersectLogs.clear();
        }
        int getCode(float x, float y) {
            int c = 0;
            if (x < csXMin) c |= 1; else if (x > csXMax) c |= 2;
            if (y < csYMin) c |= 4; else if (y > csYMax) c |= 8;
            return c;
        }
        String getCodeStr(int c) { return String.format("%4s", Integer.toBinaryString(c)).replace(' ', '0'); }
        void calcInstant() { init(); while(!done) step(); }
        void step() {
            if(done) return;
            if(animState == 0) { animSteps.add("Uç noktaların bölge kodları hesaplandı."); animState++; return; }
            if(animState == 1) { animSteps.add("OR testi yapıldı: " + getCodeStr(origC1|origC2)); animState++; return; }
            if(animState == 2) { animSteps.add("AND testi yapıldı: " + getCodeStr(origC1&origC2)); animState++; return; }
            
            c1 = getCode(cx1, cy1); c2 = getCode(cx2, cy2);
            
            if ((c1 | c2) == 0) {
                done = true; accepted = true; 
                if(animState == 3) animSteps.add("Karar: Trivial Accept (Kabul edildi)");
                animSteps.add("Nihai sonuç: İçeride."); 
                return;
            }
            if ((c1 & c2) != 0) {
                done = true; accepted = false; 
                if(animState == 3) animSteps.add("Karar: Trivial Reject (Reddedildi)");
                animSteps.add("Nihai sonuç: Tamamen dışarıda.");
                return;
            }
            if (animState == 3) { animSteps.add("Karar: Kısmi Kırpma. Kesişim aranıyor..."); animState++; return; }

            int out = (c1 != 0) ? c1 : c2;
            float ix = 0, iy = 0;
            float dx = cx2 - cx1, dy = cy2 - cy1;
            String kn = "";

            if ((out & 8) != 0) { ix = cx1 + (dx != 0 && dy != 0 ? dx * (csYMax - cy1) / dy : 0); iy = csYMax; kn = "Üst"; }
            else if ((out & 4) != 0) { ix = cx1 + (dx != 0 && dy != 0 ? dx * (csYMin - cy1) / dy : 0); iy = csYMin; kn = "Alt"; }
            else if ((out & 2) != 0) { iy = cy1 + (dx != 0 && dy != 0 ? dy * (csXMax - cx1) / dx : 0); ix = csXMax; kn = "Sağ"; }
            else if ((out & 1) != 0) { iy = cy1 + (dx != 0 && dy != 0 ? dy * (csXMin - cx1) / dx : 0); ix = csXMin; kn = "Sol"; }
            
            String iLog = String.format(Locale.US, "%s(%.1f, %.1f)", kn, ix, iy);
            intersectLogs.add(iLog);
            animSteps.add("Kesişim: " + iLog);
            
            if (out == c1) { cx1 = ix; cy1 = iy; } else { cx2 = ix; cy2 = iy; }
            c1 = getCode(cx1, cy1); c2 = getCode(cx2, cy2);
        }
    }

    class PolyObj {
        List<float[]> orig = new ArrayList<>(), curr = new ArrayList<>();
        int step = 0; 
        String log = "Bekliyor.";
        PolyObj(List<float[]> pts) { 
            for(float[] p:pts) orig.add(new float[]{p[0],p[1]});
            curr.addAll(orig); calcInstant();
        }
        void calcInstant() { curr.clear(); curr.addAll(orig); step=0; while(step<=3) step(); }
        boolean isInside(float[] p, int edge) {
            if(edge==0) return p[0]>=shXMin; if(edge==1) return p[0]<=shXMax;
            if(edge==2) return p[1]>=shYMin; return p[1]<=shYMax;
        }
        float[] getIntersect(float[] p1, float[] p2, int edge) {
            float x1=p1[0], y1=p1[1], x2=p2[0], y2=p2[1], ix=0, iy=0, dx=x2-x1, dy=y2-y1;
            if(dx==0) dx=0.0001f; if(dy==0) dy=0.0001f;
            if(edge==0) { ix=shXMin; iy=y1+dy*(shXMin-x1)/dx; }
            else if(edge==1) { ix=shXMax; iy=y1+dy*(shXMax-x1)/dx; }
            else if(edge==2) { iy=shYMin; ix=x1+dx*(shYMin-y1)/dy; }
            else if(edge==3) { iy=shYMax; ix=x1+dx*(shYMax-y1)/dy; }
            return new float[]{ix, iy};
        }
        void step() {
            if(step>3) return;
            List<float[]> nxt = new ArrayList<>();
            if(curr.size()>0) {
                float[] S = curr.get(curr.size()-1);
                for(float[] E : curr) {
                    boolean sIn = isInside(S, step), eIn = isInside(E, step);
                    if(sIn && eIn) nxt.add(E);
                    else if(sIn && !eIn) nxt.add(getIntersect(S,E,step));
                    else if(!sIn && eIn) { nxt.add(getIntersect(S,E,step)); nxt.add(E); }
                    S = E;
                }
            }
            curr = nxt; 
            String kenarAd = "";
            if(step==0) kenarAd = "Sol kenar x >= " + shXMin;
            else if(step==1) kenarAd = "Sağ kenar x <= " + shXMax;
            else if(step==2) kenarAd = "Alt kenar y >= " + shYMin;
            else if(step==3) kenarAd = "Üst kenar y <= " + shYMax;
            step++;
            
            StringBuilder pts = new StringBuilder();
            for(int i=0; i<Math.min(3, curr.size()); i++) pts.append(String.format(Locale.US, "(%.1f, %.1f) ", curr.get(i)[0], curr.get(i)[1]));
            if(curr.size()>3) pts.append("... Toplam ").append(curr.size()).append(" nokta");
            
            log = "Adım " + step + ": " + kenarAd + " işlendi. Ara Liste: " + pts.toString();
        }
        float getArea(List<float[]> pList) {
            if(pList.size()<3) return 0;
            float s1=0, s2=0; int n=pList.size();
            for(int i=0; i<n; i++) { s1+=pList.get(i)[0]*pList.get((i+1)%n)[1]; s2+=pList.get(i)[1]*pList.get((i+1)%n)[0]; }
            return Math.abs(s1-s2)/2f;
        }
    }

    void checkHover(float mx, float my) {
        if (!vMode || mode == 1) { hoverState = -1; cursor(ARROW); return; }
        float cxMin = getCurCMinX(), cxMax = getCurCMaxX(), cyMin = getCurCMinY(), cyMax = getCurCMaxY();
        float sxMin = w2sX(cxMin, lX, pW, getCurWMinX(), getCurWMaxX());
        float sxMax = w2sX(cxMax, lX, pW, getCurWMinX(), getCurWMaxX());
        float syMax = w2sY(cyMin, pY, pH, getCurWMinY(), getCurWMaxY()); 
        float syMin = w2sY(cyMax, pY, pH, getCurWMinY(), getCurWMaxY());
        
        float tol = 15;
        boolean onL = abs(mx - sxMin) < tol, onR = abs(mx - sxMax) < tol;
        boolean onT = abs(my - syMin) < tol, onB = abs(my - syMax) < tol;
        boolean inY = my >= syMin - tol && my <= syMax + tol;
        boolean inX = mx >= sxMin - tol && mx <= sxMax + tol;

        if (onL && onT) hoverState = 4; else if (onR && onT) hoverState = 5;
        else if (onL && onB) hoverState = 6; else if (onR && onB) hoverState = 7;
        else if (onL && inY) hoverState = 0; else if (onR && inY) hoverState = 1;
        else if (onT && inX) hoverState = 2; else if (onB && inX) hoverState = 3;
        else hoverState = -1;

        if(hoverState >= 4) cursor(CROSS); else if(hoverState >= 0) cursor(MOVE); else cursor(ARROW);
    }

    @Override
    public void mouseMoved() { checkHover(mouseX, mouseY); }

    @Override
    public void mousePressed() {
        if(mouseX>=lX && mouseX<=lX+pW && mouseY>=pY && mouseY<=pY+pH) {
            float wx = s2wX(mouseX, lX, pW, getCurWMinX(), getCurWMaxX());
            float wy = s2wY(mouseY, pY, pH, getCurWMinY(), getCurWMaxY());
            if(vMode && hoverState != -1) {
                dragState = hoverState;
            } else if (!vMode) {
                if(mode == 2) { isDrawingLine=true; tempX=wx; tempY=wy; }
                else if(mode == 3) { isDrawingPoly=true; customPoly.add(new float[]{wx,wy}); }
            }
        }
    }

    @Override
    public void mouseDragged() {
        if(dragState != -1 && vMode) {
            float wx = s2wX(mouseX, lX, pW, getCurWMinX(), getCurWMaxX());
            float wy = s2wY(mouseY, pY, pH, getCurWMinY(), getCurWMaxY());
            float minS = 1.0f;
            if(mode == 2) {
                if((dragState==0||dragState==4||dragState==6) && wx < csXMax-minS) csXMin=wx;
                if((dragState==1||dragState==5||dragState==7) && wx > csXMin+minS) csXMax=wx;
                if((dragState==3||dragState==6||dragState==7) && wy < csYMax-minS) csYMin=wy;
                if((dragState==2||dragState==4||dragState==5) && wy > csYMin+minS) csYMax=wy;
                for(LineObj l:lines) l.calcInstant();
            } else if (mode == 3) {
                if((dragState==0||dragState==4||dragState==6) && wx < shXMax-minS) shXMin=wx;
                if((dragState==1||dragState==5||dragState==7) && wx > shXMin+minS) shXMax=wx;
                if((dragState==3||dragState==6||dragState==7) && wy < shYMax-minS) shYMin=wy;
                if((dragState==2||dragState==4||dragState==5) && wy > shYMin+minS) shYMax=wy;
                for(PolyObj p:polys) p.calcInstant();
            }
        }
    }

    @Override
    public void mouseReleased() {
        dragState = -1;
        if(isDrawingLine && mode==2) {
            isDrawingLine=false;
            float wx = s2wX(mouseX, lX, pW, getCurWMinX(), getCurWMaxX());
            float wy = s2wY(mouseY, pY, pH, getCurWMinY(), getCurWMaxY());
            addLine(tempX, tempY, wx, wy);
        }
    }

    @Override
    public void keyPressed() {
        char k = Character.toUpperCase(key);
        if(k=='1') {mode=1; perfMsg="";} else if(k=='2') {mode=2; perfMsg="";} else if(k=='3') {mode=3; perfMsg="";}
        else if(k=='V') { vMode = !vMode; checkHover(mouseX, mouseY); }
        else if(k=='R') {
            if(mode==2) { lines.clear(); addLine(2,2,8,6); perfMsg=""; }
            else if(mode==3) { polys.clear(); customPoly.clear(); polyType=0; addPoly(0); perfMsg=""; }
        }
        else if(k==' ') {
            if(mode==2 && !lines.isEmpty()) lines.get(lines.size()-1).step();
            else if(mode==3 && !polys.isEmpty()) polys.get(polys.size()-1).step();
        }
        else if(mode==2) {
            if(k=='A') { currentLineIdx=0; addLine(2,2,8,6); } 
            else if(k=='B') { currentLineIdx=1; addLine(2,4,8,4); }
            else if(k=='C') { currentLineIdx=2; addLine(5,2,5,10); }
            else if(k=='D') { currentLineIdx=3; addLine(1,1,1,5); }
            else if(k=='E') { currentLineIdx=4; addLine(0,0,10,8); }
            else if(k=='F') { currentLineIdx=5; addLine(5,5,5,5); }
            else if(k=='P') runPerfCS();
        }
        else if(mode==3) {
            if(k=='K') { polyType=0; addPoly(0); } 
            else if(k=='U') { polyType=1; addPoly(1); } 
            else if(k=='O') runPerfSH();
            else if(key==ENTER || key==RETURN) { 
                if(customPoly.size()>2) { polyType=2; polys.add(new PolyObj(customPoly)); } 
                customPoly.clear(); isDrawingPoly=false; 
            }
            else if(key==BACKSPACE || key==8) { if(customPoly.size()>0) customPoly.remove(customPoly.size()-1); }
        }
    }

    void addLine(float x1, float y1, float x2, float y2) { lines.add(new LineObj(x1,y1,x2,y2,false)); }
    void addPoly(int type) {
        List<float[]> p = new ArrayList<>();
        if(type==0) { p.add(new float[]{-1,3}); p.add(new float[]{5,-1}); p.add(new float[]{11,3}); p.add(new float[]{5,7}); }
        else { p.add(new float[]{1,1}); p.add(new float[]{9,1}); p.add(new float[]{9,4}); p.add(new float[]{6,4}); p.add(new float[]{6,2}); p.add(new float[]{4,2}); p.add(new float[]{4,4}); p.add(new float[]{1,4}); }
        polys.add(new PolyObj(p));
    }

    void runPerfCS() {
        long t0 = millis(); int acc=0, rej=0, clip=0;
        for(int i=0; i<1000; i++) {
            LineObj l = new LineObj(random(w2XMin,w2XMax), random(w2YMin,w2YMax), random(w2XMin,w2XMax), random(w2YMin,w2YMax), true);
            if(l.accepted) acc++; else if(l.x1==l.cx1 && l.y1==l.cy1 && l.x2==l.cx2 && l.y2==l.cy2) rej++; else clip++;
        }
        perfMsg = "[PERF] 1000 Çizgi -> Süre: " + (millis()-t0) + " ms (K:"+acc+" R:"+rej+" C:"+clip+")";
    }
    void runPerfSH() {
        long t0 = millis();
        for(int i=0; i<1000; i++) {
            List<float[]> rp = new ArrayList<>();
            for(int j=0; j<4; j++) rp.add(new float[]{random(w3XMin,w3XMax), random(w3YMin,w3YMax)});
            new PolyObj(rp);
        }
        perfMsg = "[PERF] 1000 Dörtgen -> Süre: " + (millis()-t0) + " ms.";
    }

    // --- MODERN ARAYÜZ (UI) ÇİZİMLERİ ---

    @Override
    public void draw() {
        background(24, 24, 36);
        if(mode==1) drawM1(); else if(mode==2) drawM2(); else drawM3();
        drawHeader();
        drawFooter();
    }

    private void drawHeader() {
        noStroke();
        fill(30, 32, 45, 240);
        rect(0, 0, width, 70);
        
        fill(255); textAlign(LEFT, CENTER); textSize(24);
        text("2D Görüntüleme ve Kırpma Editörü", 30, 35);
        
        String mStr = mode==1 ? "Görev 1: Koordinat Dönüşümü (Sabit)" :
                      mode==2 ? "Görev 2: Cohen-Sutherland" : "Görev 3: Sutherland-Hodgman";
        if(vMode && mode != 1) mStr += " [V: PENCERE DÜZENLEME AÇIK]";
        
        textSize(15);
        float tw = textWidth(mStr) + 40;
        fill(64, 169, 255, 40); stroke(64, 169, 255); strokeWeight(1);
        rect(width/2f - tw/2f, 20, tw, 30, 15);
        fill(200, 240, 255); textAlign(CENTER, CENTER); noStroke();
        text(mStr, width/2f, 33);
        
        if(!perfMsg.isEmpty()) {
            fill(76, 217, 100); textAlign(RIGHT, CENTER); textSize(14);
            text(perfMsg, width - 30, 35);
        }
    }

    private void drawFooter() {
        noStroke();
        fill(30, 32, 45, 240);
        rect(0, height - 50, width, 50);
        
        fill(170, 175, 195); textAlign(CENTER, CENTER); textSize(14);
        String txt = "";
        if(mode == 1) txt = "[2, 3] Mod Değiştir";
        else if(mode == 2) txt = "[A-F] Hazır Çizgi  |  [Fare] Çizgi Çiz  |  [Space] Adım İlerle  |  [V] Pencere Düzenle  |  [P] Performans Testi  |  [R] Temizle  |  [1-3] Mod Değiştir";
        else if(mode == 3) txt = "[K] Konveks  |  [U] Konkav  |  [Fare] Çizim  |  [Enter] Bitir  |  [Backspace] Geri  |  [Space] Adım  |  [V] Pencere  |  [O] Perf.  |  [1-3] Mod";
        
        text(txt, width/2f, height - 26);
    }

    private float mapX(float xw) { return xvMin + (xw - xwMin) * ((xvMax - xvMin) / (xwMax - xwMin)); }
    private float mapY(float yw) { return yvMin + (ywMax - yw) * ((yvMax - yvMin) / (ywMax - ywMin)); }

    void drawM1() {
        float m1pH = 330;
        fill(34, 36, 54); stroke(54, 56, 76); strokeWeight(2); 
        rect(lX, pY, pW, m1pH, 15); rect(rX, pY, pW, m1pH, 15);
        
        fill(240, 245, 255); textAlign(CENTER, TOP); textSize(17);
        text("Dünya Koordinatları (Window)", lX+pW/2, pY+15);
        text("Piksel Koordinatları (Viewport)", rX+pW/2, pY+15);

        float padX = 60, padY = 40;
        float vWMin = xwMin - padX, vWMax = xwMax + padX;
        float vHMin = ywMin - padY, vHMax = ywMax + padY;

        float ox = w2sX(0, lX, pW, vWMin, vWMax), oy = w2sY(0, pY, m1pH, vHMin, vHMax);
        stroke(80, 82, 100); strokeWeight(1); line(lX, oy, lX+pW, oy); line(ox, pY, ox, pY+m1pH);

        stroke(255, 69, 58); noFill(); strokeWeight(2); rectMode(CORNERS);
        rect(w2sX(xwMin, lX, pW, vWMin, vWMax), w2sY(ywMax, pY, m1pH, vHMin, vHMax),
             w2sX(xwMax, lX, pW, vWMin, vWMax), w2sY(ywMin, pY, m1pH, vHMin, vHMax));
        rectMode(CORNER);

        float vSX = rX + xvMin, vSY = pY + yvMin, vW = xvMax - xvMin, vH = yvMax - yvMin;
        stroke(64, 169, 255); noFill(); strokeWeight(2); rect(vSX, vSY, vW, vH, 8); strokeWeight(1);
        
        fill(64, 169, 255); textSize(13);
        textAlign(RIGHT, BOTTOM); text("(50,30)", vSX-8, vSY-8);
        textAlign(LEFT, BOTTOM); text("(430,30)", vSX+vW+8, vSY-8);
        textAlign(RIGHT, TOP); text("(50,290)", vSX-8, vSY+vH+8);
        textAlign(LEFT, TOP); text("(430,290)", vSX+vW+8, vSY+vH+8);

        for(float[] p : testPoints) {
            float pxW = w2sX(p[0], lX, pW, vWMin, vWMax), pyW = w2sY(p[1], pY, m1pH, vHMin, vHMax);
            fill(255, 204, 0); noStroke(); ellipse(pxW, pyW, 10, 10);
            fill(240, 245, 255); textAlign(LEFT, BOTTOM); text(String.format("W(%.0f, %.0f)",p[0],p[1]), pxW+8, pyW-8);

            float pxV = rX + mapX(p[0]), pyV = pY + mapY(p[1]);
            fill(76, 217, 100); ellipse(pxV, pyV, 10, 10);
            fill(240, 245, 255); text(String.format("V(%.0f,%.0f)",mapX(p[0]),mapY(p[1])), pxV+8, pyV-8);
        }

        fill(255, 204, 0); textAlign(CENTER, TOP); textSize(15);
        float ty = pY + m1pH + 25;
        text("mapX = 50 + (xw + 150) * 380 / 300   |   mapY = 290 - (yw + 100) * 260 / 200", width/2f, ty); ty += 25;
        fill(170, 175, 195); text("mapY içinde Y-flip uygulanır çünkü Processing'de Y ekseni aşağı doğru artar.", width/2f, ty); ty += 35;
        
        fill(240, 245, 255);
        text("Nokta W(x,y)         |         X_v Hesabı         |         Y_v Hesabı         |         Sonuç V(x,y)", width/2f, ty); ty += 22;
        stroke(80, 82, 100); line(width/2f - 350, ty, width/2f + 350, ty); ty += 12;
        fill(170, 175, 195);
        for(float[] p : testPoints) {
            text(String.format("W(%.0f, %.0f)         |         %.0f         |         %.0f         |         V(%.0f, %.0f)", 
                p[0], p[1], mapX(p[0]), mapY(p[1]), mapX(p[0]), mapY(p[1])), width/2f, ty);
            ty += 22;
        }
    }

    void drawClipWindow(float x, float wX, float wW) {
        stroke(54, 56, 76); strokeWeight(1);
        for(int i=(int)getCurWMinX(); i<=(int)getCurWMaxX(); i+=2) {
            line(w2sX(i, x, wW, getCurWMinX(), getCurWMaxX()), pY+1, w2sX(i, x, wW, getCurWMinX(), getCurWMaxX()), pY+pH-1);
            line(x+1, w2sY(i, pY, pH, getCurWMinY(), getCurWMaxY()), x+wW-1, w2sY(i, pY, pH, getCurWMinY(), getCurWMaxY()));
        }
        stroke(vMode ? color(255, 204, 0) : color(64, 169, 255)); noFill(); strokeWeight(vMode?3:2); rectMode(CORNERS);
        float pxMin = w2sX(getCurCMinX(), x, wW, getCurWMinX(), getCurWMaxX());
        float pxMax = w2sX(getCurCMaxX(), x, wW, getCurWMinX(), getCurWMaxX());
        float pyMin = w2sY(getCurCMaxY(), pY, pH, getCurWMinY(), getCurWMaxY());
        float pyMax = w2sY(getCurCMinY(), pY, pH, getCurWMinY(), getCurWMaxY());
        rect(pxMin, pyMin, pxMax, pyMax);
        rectMode(CORNER); strokeWeight(1);

        if(vMode && x==lX && hoverState!=-1) {
            stroke(255, 69, 58); strokeWeight(5);
            if(hoverState==0||hoverState==4||hoverState==6) line(pxMin, pyMin, pxMin, pyMax);
            if(hoverState==1||hoverState==5||hoverState==7) line(pxMax, pyMin, pxMax, pyMax);
            if(hoverState==2||hoverState==4||hoverState==5) line(pxMin, pyMin, pxMax, pyMin);
            if(hoverState==3||hoverState==6||hoverState==7) line(pxMin, pyMax, pxMax, pyMax);
            strokeWeight(1);
        }
    }

    void drawM2() {
        fill(34, 36, 54); stroke(54, 56, 76); strokeWeight(2); 
        rect(lX, pY, pW, pH, 15); rect(rX, pY, pW, pH, 15);
        fill(240, 245, 255); textAlign(CENTER, TOP); textSize(17);
        text("Orijinal Çizgiler & Pencere", lX+pW/2, pY+15); text("Kırpılmış Sonuç & Cohen-Sutherland Analizi", rX+pW/2, pY+15);
        drawClipWindow(lX, pW, pW); drawClipWindow(rX, pW, pW);

        LineObj al = lines.isEmpty() ? null : lines.get(lines.size()-1);

        for(LineObj l : lines) {
            stroke(100, 105, 120); strokeWeight(2);
            line(w2sX(l.x1,lX,pW,w2XMin,w2XMax), w2sY(l.y1,pY,pH,w2YMin,w2YMax), w2sX(l.x2,lX,pW,w2XMin,w2XMax), w2sY(l.y2,pY,pH,w2YMin,w2YMax));
            if(!l.done) stroke(255, 204, 0);
            else if(l.accepted) stroke(76, 217, 100); else continue;
            strokeWeight(4);
            line(w2sX(l.cx1,rX,pW,w2XMin,w2XMax), w2sY(l.cy1,pY,pH,w2YMin,w2YMax), w2sX(l.cx2,rX,pW,w2XMin,w2XMax), w2sY(l.cy2,pY,pH,w2YMin,w2YMax));
            fill(255, 204, 0); noStroke();
            ellipse(w2sX(l.cx1,rX,pW,w2XMin,w2XMax), w2sY(l.cy1,pY,pH,w2YMin,w2YMax), 10, 10);
            ellipse(w2sX(l.cx2,rX,pW,w2XMin,w2XMax), w2sY(l.cy2,pY,pH,w2YMin,w2YMax), 10, 10);
        }

        if(al != null && al.done && !al.accepted) {
            fill(255, 69, 58, 200); textAlign(CENTER, CENTER); textSize(36);
            text("REJECTED", rX + pW/2f, pY + pH/2f);
        }

        if(isDrawingLine) {
            stroke(240, 245, 255); strokeWeight(2);
            line(w2sX(tempX,lX,pW,w2XMin,w2XMax), w2sY(tempY,pY,pH,w2YMin,w2YMax), mouseX, mouseY);
        }

        if(al != null) {
            float ty = pY + 50;
            fill(240, 245, 255); textAlign(LEFT, TOP); textSize(14);
            String lName = currentLineIdx >= 0 && currentLineIdx < 6 ? String.valueOf((char)('A' + currentLineIdx)) : "Özel";
            
            fill(64, 169, 255, 40); stroke(64, 169, 255); strokeWeight(1);
            rect(rX + 20, ty - 5, textWidth("Seçili Çizgi: " + lName) + 20, 26, 8);
            fill(64, 169, 255); noStroke();
            text("Seçili Çizgi: " + lName, rX+30, ty); ty += 35;
            
            fill(170, 175, 195);
            text(String.format(Locale.US, "Orijinal: P1(%.1f, %.1f) -> P2(%.1f, %.1f)", al.x1, al.y1, al.x2, al.y2), rX+20, ty); ty += 22;
            text("P1 Code: " + al.getCodeStr(al.origC1) + "   |   P2 Code: " + al.getCodeStr(al.origC2), rX+20, ty); ty += 22;
            text("OR = " + al.getCodeStr(al.origC1 | al.origC2) + "   |   AND = " + al.getCodeStr(al.origC1 & al.origC2), rX+20, ty); ty += 22;
            
            String karar = (al.origC1 | al.origC2) == 0 ? "Trivial Accept" : (al.origC1 & al.origC2) != 0 ? "Trivial Reject" : "Kısmi Kırpma Gerekiyor";
            fill(255, 204, 0); text("Karar: " + karar, rX+20, ty); ty += 22;
            
            fill(170, 175, 195);
            String kes = al.intersectLogs.isEmpty() ? "Yok" : String.join(", ", al.intersectLogs);
            text("Kesişim: " + kes, rX+20, ty); ty += 22;
            
            if(al.done) {
                fill(al.accepted ? color(76, 217, 100) : color(255, 69, 58));
                if(al.accepted) text(String.format(Locale.US, "Sonuç: (%.1f, %.1f) -> (%.1f, %.1f)", al.cx1, al.cy1, al.cx2, al.cy2), rX+20, ty);
                else text("Sonuç: Çizgi dışarıda atıldı.", rX+20, ty);
            } else {
                fill(64, 169, 255); text("Sonuç: Hesaplanıyor... (Space'e basın)", rX+20, ty);
            }

            ty += 40;
            fill(240, 245, 255); textSize(15); text("Animasyon Adımları:", rX+20, ty); ty += 25;
            fill(170, 175, 195); textSize(13);
            for(String s : al.animSteps) { text("• " + s, rX+20, ty); ty += 20; }
        }
    }

    void drawM3() {
        fill(34, 36, 54); stroke(54, 56, 76); strokeWeight(2); 
        rect(lX, pY, pW, pH, 15); 
        rect(rX, pY, 360, pH, 15); 
        
        fill(240, 245, 255); textAlign(CENTER, TOP); textSize(17);
        String pTip = polyType==0 ? "Konveks" : polyType==1 ? "Konkav U" : "Özel";
        text("Orijinal Poligon (" + pTip + ")", lX+pW/2, pY+15);
        text("Kırpılmış Sonuçlar", rX+180, pY+15);
        
        drawClipWindow(lX, pW, pW); drawClipWindow(rX, pW, 360);

        for(PolyObj p : polys) {
            stroke(100, 105, 120); strokeWeight(2); fill(100, 105, 120, 60);
            beginShape(); for(float[] pt:p.orig) vertex(w2sX(pt[0],lX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax)); endShape(CLOSE);
            
            stroke(76, 217, 100); strokeWeight(3); fill(76, 217, 100, 60);
            beginShape(); for(float[] pt:p.curr) vertex(w2sX(pt[0],rX,360,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax)); endShape(CLOSE);
            
            fill(255, 204, 0); noStroke();
            for(float[] pt:p.curr) {
                float px = w2sX(pt[0],rX,360,w3XMin,w3XMax);
                float py = w2sY(pt[1],pY,pH,w3YMin,w3YMax);
                ellipse(px, py, 10, 10);
                if(abs(pt[0]-shXMin)<0.1 || abs(pt[0]-shXMax)<0.1 || abs(pt[1]-shYMin)<0.1 || abs(pt[1]-shYMax)<0.1) {
                    fill(255, 204, 0); textSize(11); textAlign(LEFT,BOTTOM); text("Kesişim", px+6, py-6);
                }
            }
        }

        if(isDrawingPoly && customPoly.size()>0) {
            stroke(240, 245, 255); strokeWeight(2); noFill();
            beginShape(); for(float[] pt:customPoly) vertex(w2sX(pt[0],lX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax));
            vertex(mouseX, mouseY); endShape();
        }

        if(!polys.isEmpty()) {
            PolyObj ap = polys.get(polys.size()-1);
            float oAlan = ap.getArea(ap.orig), kAlan = ap.getArea(ap.curr), fark = Math.abs(oAlan - kAlan);
            
            fill(255, 204, 0); textAlign(CENTER, TOP); textSize(15);
            text("Log: " + ap.log, width/2f, pY+pH+20);
            
            fill(76, 217, 100);
            text(String.format(Locale.US, "Orijinal Alan: %.1f   |   Kırpılmış Alan: %.1f   |   Fark: %.1f", oAlan, kAlan, fark), width/2f, pY+pH+45);

            if(polyType == 1) {
                fill(255, 69, 58); textSize(13);
                text("Uyarı: Konkav poligonlarda Sutherland-Hodgman'ın topoloji yorumu dikkatli yapılmalıdır.", width/2f, pY+pH+70);
            }
            
            fill(240, 245, 255); textAlign(LEFT, TOP); textSize(14);
            float ty = pY+50; float listX = rX + 360 + 30;
            text("Kırpılmış Noktalar:", listX, ty); ty+=25;
            fill(76, 217, 100); textSize(13);
            int maxList = Math.min(ap.curr.size(), 14); 
            for(int i=0; i<maxList; i++) {
                text(String.format(Locale.US, "(%.2f, %.2f)", ap.curr.get(i)[0], ap.curr.get(i)[1]), listX, ty); ty+=22;
            }
            if(ap.curr.size() > maxList) {
                fill(170, 175, 195); text("... + " + (ap.curr.size() - maxList) + " nokta daha", listX, ty);
            }
        }
    }
}
