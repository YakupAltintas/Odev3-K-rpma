package com.grafikler.editor;

import processing.core.PApplet;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainApp extends PApplet {

    int mode = 1;
    boolean vMode = false; // Pencere düzenleme modu
    int hoverState = -1; // -1:Yok, 0:Sol, 1:Sağ, 2:Üst, 3:Alt, 4:SolÜst, 5:SağÜst, 6:SolAlt, 7:SağAlt
    int dragState = -1;
    String perfMsg = "";

    // Ekran panelleri
    float pW = 550, pH = 480;
    float lX = 50, rX = 650, pY = 100;

    // Görev 1 (Sabit)
    float xwMin = -150, xwMax = 150, ywMin = -100, ywMax = 100;
    float xvMin = 50, xvMax = 430, yvMin = 30, yvMax = 290;
    float[][] testPoints = {{0,0}, {150,100}, {-150,-100}, {75,-50}, {-30,80}};

    // Görev 2 Dünya ve Pencere
    float w2XMin = -2, w2XMax = 12, w2YMin = -2, w2YMax = 12;
    float csXMin = 2, csXMax = 8, csYMin = 2, csYMax = 6;
    List<LineObj> lines = new ArrayList<>();
    boolean isDrawingLine = false;
    float tempX, tempY;

    // Görev 3 Dünya ve Pencere
    float w3XMin = -4, w3XMax = 14, w3YMin = -4, w3YMax = 14;
    float shXMin = 0, shXMax = 10, shYMin = 0, shYMax = 10;
    List<PolyObj> polys = new ArrayList<>();
    boolean isDrawingPoly = false;
    List<float[]> customPoly = new ArrayList<>();

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

    // --- KOORDİNAT DÖNÜŞÜMLERİ --- 
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

    // --- SINIFLAR (C-S Çizgi ve S-H Poligon) ---
    class LineObj {
        float x1, y1, x2, y2, cx1, cy1, cx2, cy2;
        boolean done, accepted;
        String log = "Bekliyor...";
        int c1, c2;
        LineObj(float x1, float y1, float x2, float y2) { 
            this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2; 
            this.cx1=x1; this.cy1=y1; this.cx2=x2; this.cy2=y2;
            calcInstant();
        }
        int getCode(float x, float y) {
            int c = 0;
            if (x < csXMin) c |= 1; else if (x > csXMax) c |= 2;
            if (y < csYMin) c |= 4; else if (y > csYMax) c |= 8;
            return c;
        }
        String getCodeStr(int c) { return String.format("%4s", Integer.toBinaryString(c)).replace(' ', '0'); }
        void calcInstant() {
            cx1=x1; cy1=y1; cx2=x2; cy2=y2; done=false; accepted=false;
            while(!done) step();
        }
        void reset() { cx1=x1; cy1=y1; cx2=x2; cy2=y2; done=false; accepted=false; log="Sıfırlandı."; c1=getCode(cx1,cy1); c2=getCode(cx2,cy2); }
        void step() {
            if(done) return;
            c1 = getCode(cx1, cy1); c2 = getCode(cx2, cy2);
            String s1=getCodeStr(c1), s2=getCodeStr(c2);
            if ((c1 | c2) == 0) { done = true; accepted = true; log="KABUL (P1:"+s1+" P2:"+s2+" OR:0000)"; return; }
            if ((c1 & c2) != 0) { done = true; accepted = false; log="RET (P1:"+s1+" P2:"+s2+" AND:"+getCodeStr(c1&c2)+")"; return; }
            int out = (c1 != 0) ? c1 : c2;
            float ix=0, iy=0, dx=cx2-cx1, dy=cy2-cy1;
            if(dx==0) dx=0.0001f; if(dy==0) dy=0.0001f;
            if ((out & 8) != 0) { ix = cx1 + dx*(csYMax-cy1)/dy; iy = csYMax; log="Üstten kesildi."; }
            else if ((out & 4) != 0) { ix = cx1 + dx*(csYMin-cy1)/dy; iy = csYMin; log="Alttan kesildi."; }
            else if ((out & 2) != 0) { iy = cy1 + dy*(csXMax-cx1)/dx; ix = csXMax; log="Sağdan kesildi."; }
            else if ((out & 1) != 0) { iy = cy1 + dy*(csXMin-cx1)/dx; ix = csXMin; log="Soldan kesildi."; }
            if (out == c1) { cx1=ix; cy1=iy; } else { cx2=ix; cy2=iy; }
            c1 = getCode(cx1, cy1); c2 = getCode(cx2, cy2);
        }
    }

    class PolyObj {
        List<float[]> orig = new ArrayList<>(), curr = new ArrayList<>();
        int step = 0; String log = "Bekliyor.";
        PolyObj(List<float[]> pts) { 
            for(float[] p:pts) orig.add(new float[]{p[0],p[1]});
            curr.addAll(orig); calcInstant();
        }
        void reset() { curr.clear(); curr.addAll(orig); step=0; log="Sıfırlandı."; }
        void calcInstant() { reset(); while(step<=3) step(); }
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
            curr = nxt; step++;
            log = "Adım " + step + " (Kenar " + (step-1) + ") tamamlandı.";
        }
        float getArea(List<float[]> pList) {
            if(pList.size()<3) return 0;
            float s1=0, s2=0; int n=pList.size();
            for(int i=0; i<n; i++) { s1+=pList.get(i)[0]*pList.get((i+1)%n)[1]; s2+=pList.get(i)[1]*pList.get((i+1)%n)[0]; }
            return Math.abs(s1-s2)/2f;
        }
    }

    // --- MOUSE VE PENCERE SÜRÜKLEME --- 
    void checkHover(float mx, float my) {
        if (!vMode || mode == 1) { hoverState = -1; cursor(ARROW); return; }
        float wx = s2wX(mx, lX, pW, getCurWMinX(), getCurWMaxX());
        float wy = s2wY(my, pY, pH, getCurWMinY(), getCurWMaxY());
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

    // --- TUŞ KONTROLLERİ VE TESTLER ---
    @Override
    public void keyPressed() {
        char k = Character.toUpperCase(key);
        if(k=='1') {mode=1; perfMsg="";} else if(k=='2') {mode=2; perfMsg="";} else if(k=='3') {mode=3; perfMsg="";}
        else if(k=='V') { vMode = !vMode; checkHover(mouseX, mouseY); }
        else if(k=='R') {
            if(mode==2) { lines.clear(); addLine(2,2,8,6); perfMsg=""; }
            else if(mode==3) { polys.clear(); customPoly.clear(); addPoly(0); perfMsg=""; }
        }
        else if(k==' ') {
            if(mode==2 && !lines.isEmpty()) lines.get(lines.size()-1).step();
            else if(mode==3 && !polys.isEmpty()) polys.get(polys.size()-1).step();
        }
        else if(mode==2) {
            if(k=='A') addLine(2,2,8,6); else if(k=='B') addLine(2,4,8,4); else if(k=='C') addLine(5,2,5,10);
            else if(k=='D') addLine(1,1,1,5); else if(k=='E') addLine(0,0,10,8); else if(k=='F') addLine(5,5,5,5);
            else if(k=='P') runPerfCS();
        }
        else if(mode==3) {
            if(k=='K') addPoly(0); else if(k=='U') addPoly(1); else if(k=='O') runPerfSH();
            else if(key==ENTER || key==RETURN) { 
                if(customPoly.size()>2) polys.add(new PolyObj(customPoly)); 
                customPoly.clear(); isDrawingPoly=false; 
            }
            else if(key==BACKSPACE || key==8) { if(customPoly.size()>0) customPoly.remove(customPoly.size()-1); }
        }
    }

    void addLine(float x1, float y1, float x2, float y2) { lines.add(new LineObj(x1,y1,x2,y2)); }
    void addPoly(int type) {
        List<float[]> p = new ArrayList<>();
        if(type==0) { p.add(new float[]{-1,3}); p.add(new float[]{5,-1}); p.add(new float[]{11,3}); p.add(new float[]{5,7}); }
        else { p.add(new float[]{1,1}); p.add(new float[]{9,1}); p.add(new float[]{9,4}); p.add(new float[]{6,4}); p.add(new float[]{6,2}); p.add(new float[]{4,2}); p.add(new float[]{4,4}); p.add(new float[]{1,4}); }
        polys.add(new PolyObj(p));
    }

    void runPerfCS() {
        long t0 = millis(); int acc=0, rej=0, clip=0;
        for(int i=0; i<1000; i++) {
            LineObj l = new LineObj(random(w2XMin,w2XMax), random(w2YMin,w2YMax), random(w2XMin,w2XMax), random(w2YMin,w2YMax));
            if(l.accepted) acc++; else if(l.x1==l.cx1 && l.y1==l.cy1 && l.x2==l.cx2 && l.y2==l.cy2) rej++; else clip++;
        }
        perfMsg = "[PERF] 1000 Çizgi Kırpıldı -> Süre: " + (millis()-t0) + " ms. (Kabul:"+acc+" Ret:"+rej+" Kırpılan:"+clip+")";
    }
    void runPerfSH() {
        long t0 = millis();
        for(int i=0; i<1000; i++) {
            List<float[]> rp = new ArrayList<>();
            for(int j=0; j<4; j++) rp.add(new float[]{random(w3XMin,w3XMax), random(w3YMin,w3YMax)});
            new PolyObj(rp);
        }
        perfMsg = "[PERF] 1000 Rastgele Dörtgen Kırpıldı -> Süre: " + (millis()-t0) + " ms.";
    }

    // --- ÇİZİM DÖNGÜSÜ ---
    @Override
    public void draw() {
        background(25);
        drawHeader();

        if(mode==1) drawM1(); else if(mode==2) drawM2(); else drawM3();

        stroke(80); line(30, height-50, width-30, height-50);
        fill(200); textAlign(CENTER, BOTTOM); textSize(14);
        text("[1-3] Mod  |  [V] Edit Modu  |  [Space] Animasyon Adımı  |  [R] Temizle\n" +
             "[M2] A-F:Çizgi, P:Perf.Test  |  [M3] K:Konveks, U:Konkav, O:Perf.Test, Enter:Poligon Bitir", width/2f, height-15);
    }

    private void drawHeader() {
        fill(255); textAlign(CENTER, TOP); textSize(24);
        text("2D Görüntüleme ve Kırpma Editörü", width/2f, 15);
        textSize(14); fill(180);
        String mStr = mode==1 ? "Görev 1: Sabit window/viewport dönüşümü" :
                      mode==2 ? "2 (Cohen-Sutherland)" : "3 (Sutherland-Hodgman)";
        String vStr = (mode!=1 && vMode) ? " [V: PENCERE DÜZENLEME AÇIK]" : "";
        text("Mod: " + mStr + vStr + "   " + perfMsg, width/2f, 45);
        stroke(80); line(30, 70, width-30, 70);
    }

    private float mapX(float xw) { return xvMin + (xw - xwMin) * ((xvMax - xvMin) / (xwMax - xwMin)); }
    private float mapY(float yw) { return yvMin + (ywMax - yw) * ((yvMax - yvMin) / (ywMax - ywMin)); }

    void drawM1() {
        fill(220); textAlign(CENTER, TOP); textSize(14);
        text("Bu modda verilen 5 nokta, window koordinatlarından viewport piksel koordinatlarına dönüştürülür.", width/2f, 75);

        float m1pH = 330;
        fill(35); stroke(80); rect(lX, pY, pW, m1pH, 5); rect(rX, pY, pW, m1pH, 5);
        fill(255); textAlign(CENTER, TOP); textSize(16);
        text("Dünya Koordinatları (Window)", lX+pW/2, pY-25);
        text("Piksel Koordinatları (Viewport)", rX+pW/2, pY-25);

        float padX = 50, padY = 30;
        float vWMin = xwMin - padX, vWMax = xwMax + padX;
        float vHMin = ywMin - padY, vHMax = ywMax + padY;

        float ox = w2sX(0, lX, pW, vWMin, vWMax), oy = w2sY(0, pY, m1pH, vHMin, vHMax);
        stroke(80); line(lX, oy, lX+pW, oy); line(ox, pY, ox, pY+m1pH);

        stroke(200, 100, 100); noFill(); rectMode(CORNERS);
        rect(w2sX(xwMin, lX, pW, vWMin, vWMax), w2sY(ywMax, pY, m1pH, vHMin, vHMax),
             w2sX(xwMax, lX, pW, vWMin, vWMax), w2sY(ywMin, pY, m1pH, vHMin, vHMax));
        rectMode(CORNER);

        float vSX = rX + xvMin;
        float vSY = pY + yvMin;
        float vW = xvMax - xvMin;
        float vH = yvMax - yvMin;
        stroke(0,150,255); noFill(); strokeWeight(2); rect(vSX, vSY, vW, vH); strokeWeight(1);
        
        fill(0,150,255); textSize(12);
        textAlign(RIGHT, BOTTOM); text("(50,30)", vSX-5, vSY-5);
        textAlign(LEFT, BOTTOM); text("(430,30)", vSX+vW+5, vSY-5);
        textAlign(RIGHT, TOP); text("(50,290)", vSX-5, vSY+vH+5);
        textAlign(LEFT, TOP); text("(430,290)", vSX+vW+5, vSY+vH+5);

        for(float[] p : testPoints) {
            float pxW = w2sX(p[0], lX, pW, vWMin, vWMax), pyW = w2sY(p[1], pY, m1pH, vHMin, vHMax);
            fill(255,255,0); noStroke(); ellipse(pxW, pyW, 8, 8);
            fill(200); textAlign(LEFT, BOTTOM); text(String.format("W(%.0f, %.0f)",p[0],p[1]), pxW+5, pyW-5);

            float pxV = rX + mapX(p[0]);
            float pyV = pY + mapY(p[1]);
            fill(0,255,0); ellipse(pxV, pyV, 8, 8);
            fill(200); text(String.format("W(%.0f,%.0f) -> V(%.0f,%.0f)",p[0],p[1],mapX(p[0]),mapY(p[1])), pxV+5, pyV-5);
        }

        fill(255,255,0); textAlign(CENTER, TOP); textSize(14);
        float ty = pY + m1pH + 15;
        text("mapX = 50 + (xw + 150) * 380 / 300   |   mapY = 290 - (yw + 100) * 260 / 200", width/2f, ty); ty += 20;
        fill(200); text("mapY içinde Y-flip uygulanır çünkü Processing'de Y ekseni aşağı doğru artar.", width/2f, ty); ty += 30;
        
        fill(255);
        text("Nokta W(x,y)      |      X_v Hesabı      |      Y_v Hesabı      |      Sonuç V(x,y)", width/2f, ty); ty += 20;
        stroke(100); line(width/2f - 300, ty, width/2f + 300, ty); ty += 10;
        fill(200);
        for(float[] p : testPoints) {
            text(String.format("W(%.0f, %.0f)      |      %.0f      |      %.0f      |      V(%.0f, %.0f)", 
                p[0], p[1], mapX(p[0]), mapY(p[1]), mapX(p[0]), mapY(p[1])), width/2f, ty);
            ty += 20;
        }
    }

    void drawClipWindow(float x, float wX, float wW) {
        stroke(60); strokeWeight(1);
        for(int i=(int)getCurWMinX(); i<=(int)getCurWMaxX(); i+=2) {
            line(w2sX(i, x, wW, getCurWMinX(), getCurWMaxX()), pY, w2sX(i, x, wW, getCurWMinX(), getCurWMaxX()), pY+pH);
            line(x, w2sY(i, pY, pH, getCurWMinY(), getCurWMaxY()), x+wW, w2sY(i, pY, pH, getCurWMinY(), getCurWMaxY()));
        }
        stroke(vMode ? color(255,150,0) : color(0,150,255)); noFill(); strokeWeight(vMode?3:2); rectMode(CORNERS);
        float pxMin = w2sX(getCurCMinX(), x, wW, getCurWMinX(), getCurWMaxX());
        float pxMax = w2sX(getCurCMaxX(), x, wW, getCurWMinX(), getCurWMaxX());
        float pyMin = w2sY(getCurCMaxY(), pY, pH, getCurWMinY(), getCurWMaxY());
        float pyMax = w2sY(getCurCMinY(), pY, pH, getCurWMinY(), getCurWMaxY());
        rect(pxMin, pyMin, pxMax, pyMax);
        rectMode(CORNER); strokeWeight(1);

        if(vMode && x==lX && hoverState!=-1) {
            stroke(255,255,0); strokeWeight(5);
            if(hoverState==0||hoverState==4||hoverState==6) line(pxMin, pyMin, pxMin, pyMax);
            if(hoverState==1||hoverState==5||hoverState==7) line(pxMax, pyMin, pxMax, pyMax);
            if(hoverState==2||hoverState==4||hoverState==5) line(pxMin, pyMin, pxMax, pyMin);
            if(hoverState==3||hoverState==6||hoverState==7) line(pxMin, pyMax, pxMax, pyMax);
            strokeWeight(1);
        }
    }

    void drawM2() {
        fill(35); stroke(80); rect(lX, pY, pW, pH, 5); rect(rX, pY, pW, pH, 5);
        fill(255); textAlign(CENTER, TOP); textSize(16);
        text("Orijinal Çizgiler & Pencere", lX+pW/2, pY-25); text("Kırpılmış Sonuçlar & Log", rX+pW/2, pY-25);
        drawClipWindow(lX, pW, pW); drawClipWindow(rX, pW, pW);

        for(LineObj l : lines) {
            stroke(100); strokeWeight(2);
            line(w2sX(l.x1,lX,pW,w2XMin,w2XMax), w2sY(l.y1,pY,pH,w2YMin,w2YMax), w2sX(l.x2,lX,pW,w2XMin,w2XMax), w2sY(l.y2,pY,pH,w2YMin,w2YMax));
            if(!l.done) stroke(255,165,0);
            else if(l.accepted) stroke(0,255,0); else continue;
            strokeWeight(3);
            line(w2sX(l.cx1,rX,pW,w2XMin,w2XMax), w2sY(l.cy1,pY,pH,w2YMin,w2YMax), w2sX(l.cx2,rX,pW,w2XMin,w2XMax), w2sY(l.cy2,pY,pH,w2YMin,w2YMax));
            fill(255,255,0); noStroke();
            ellipse(w2sX(l.cx1,rX,pW,w2XMin,w2XMax), w2sY(l.cy1,pY,pH,w2YMin,w2YMax), 8, 8);
            ellipse(w2sX(l.cx2,rX,pW,w2XMin,w2XMax), w2sY(l.cy2,pY,pH,w2YMin,w2YMax), 8, 8);
        }
        if(isDrawingLine) {
            stroke(200); strokeWeight(2);
            line(w2sX(tempX,lX,pW,w2XMin,w2XMax), w2sY(tempY,pY,pH,w2YMin,w2YMax), mouseX, mouseY);
        }

        fill(200); textAlign(LEFT, TOP); textSize(14);
        if(!lines.isEmpty()) {
            LineObj al = lines.get(lines.size()-1);
            text("Aktif Çizgi Logu: " + al.log, rX+15, pY+15);
            text(String.format(Locale.US, "P1(%.1f, %.1f) Kod: %s", al.cx1, al.cy1, al.getCodeStr(al.c1)), rX+15, pY+35);
            text(String.format(Locale.US, "P2(%.1f, %.1f) Kod: %s", al.cx2, al.cy2, al.getCodeStr(al.c2)), rX+15, pY+55);
            text("OR: " + al.getCodeStr(al.c1|al.c2) + "  AND: " + al.getCodeStr(al.c1&al.c2), rX+15, pY+75);
        }
    }

    void drawM3() {
        fill(35); stroke(80); rect(lX, pY, pW, pH, 5); rect(rX, pY, pW, pH, 5);
        fill(255); textAlign(CENTER, TOP); textSize(16);
        text("Orijinal Poligonlar", lX+pW/2, pY-25); text("Kırpılmış Sonuçlar", rX+pW/2, pY-25);
        drawClipWindow(lX, pW, pW); drawClipWindow(rX, pW, pW);

        for(PolyObj p : polys) {
            stroke(150); strokeWeight(2); fill(150,150,150,80);
            beginShape(); for(float[] pt:p.orig) vertex(w2sX(pt[0],lX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax)); endShape(CLOSE);
            stroke(0,255,0); strokeWeight(3); fill(0,255,0,80);
            beginShape(); for(float[] pt:p.curr) vertex(w2sX(pt[0],rX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax)); endShape(CLOSE);
            fill(255,255,0); noStroke();
            for(float[] pt:p.curr) ellipse(w2sX(pt[0],rX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax), 8, 8);
        }

        if(isDrawingPoly && customPoly.size()>0) {
            stroke(200); strokeWeight(2); noFill();
            beginShape(); for(float[] pt:customPoly) vertex(w2sX(pt[0],lX,pW,w3XMin,w3XMax), w2sY(pt[1],pY,pH,w3YMin,w3YMax));
            vertex(mouseX, mouseY); endShape();
        }

        if(!polys.isEmpty()) {
            PolyObj ap = polys.get(polys.size()-1);
            fill(255,255,0); textAlign(CENTER, TOP); textSize(14);
            text("Aktif Log: " + ap.log, width/2f, pY+pH+10);
            text(String.format(Locale.US, "Orijinal Alan: %.1f  |  Kırpılmış Alan: %.1f", ap.getArea(ap.orig), ap.getArea(ap.curr)), width/2f, pY+pH+30);
        }
    }
}
