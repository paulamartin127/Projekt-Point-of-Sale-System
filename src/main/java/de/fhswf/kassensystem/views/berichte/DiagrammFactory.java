package de.fhswf.kassensystem.views.berichte;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Fabrikklasse für Canvas-basierte Diagramme und zugehörige UI-Elemente.
 *
 * <p>Erzeugt Balken- und Tortendiagramme über das HTML-{@code <canvas>}-Element
 * mit inline JavaScript. Wird in {@link ZahlungsartenPanel} und
 * {@link UmsatzuebersichtPanel} verwendet.
 *
 * @author Adrian Krawietz
 */
class DiagrammFactory {

    private DiagrammFactory() {}

    /**
     * Erstellt ein Diagramm (Balken oder Torte) mit Legende.
     *
     * @param daten   2D-Array mit [Name, Wert, Farbe] pro Eintrag
     * @param balken  {@code true} für Balkendiagramm, {@code false} für Tortendiagramm
     * @return Container mit Legende und Canvas-Widget
     */
    static Div buildDiagramm(String[][] daten, boolean balken) {
        Div container = new Div();
        container.getStyle()
                .set("width", "100%").set("display", "flex").set("gap", "1.5rem").set("align-items", "center");

        double summe = 0;
        for (String[] x : daten) summe += Double.parseDouble(x[1]);
        if (summe == 0) summe = 1;

        Div legende = buildLegendeBlock(daten, summe);
        Div cw      = buildCanvasWidget(daten, balken);

        container.add(legende, cw);
        return container;
    }

    /**
     * Erstellt einen Toggle-Button für den Tag/Woche- oder Torte/Balken-Schalter.
     *
     * @param label der Beschriftungstext
     * @param aktiv {@code true} für aktiv-Styling (dunkler Hintergrund)
     * @return gestylter Button
     */
    static Button buildToggleButton(String label, boolean aktiv) {
        Button btn = new Button(label);
        btn.getStyle()
                .set("background", aktiv ? "#553722" : "transparent")
                .set("color", aktiv ? "white" : "#553722")
                .set("border", "none").set("border-radius", "9999px")
                .set("padding", "0.25rem 0.75rem").set("font-size", "0.65rem")
                .set("font-weight", "700").set("cursor", "pointer")
                .set("font-family", "'Plus Jakarta Sans', sans-serif").set("transition", "all 0.2s");
        return btn;
    }

    /**
     * Erstellt die fixe Zahlungsarten-Legende mit Bar- und Kartenfarbe.
     *
     * @return horizontales Legenden-Layout
     */
    static HorizontalLayout buildLegende() {
        HorizontalLayout legende = new HorizontalLayout();
        legende.setSpacing(false);
        legende.getStyle().set("gap", "2rem").set("justify-content", "center");
        for (String[] e : new String[][]{{"#ffdcc6", "Barzahlung"}, {"#553722", "Kartenzahlung"}}) {
            HorizontalLayout item = new HorizontalLayout();
            item.setAlignItems(FlexComponent.Alignment.CENTER);
            item.setSpacing(false);
            item.getStyle().set("gap", "0.5rem");
            Div p = new Div();
            p.getStyle().set("width", "0.75rem").set("height", "0.75rem")
                    .set("border-radius", "9999px").set("background", e[0]).set("flex-shrink", "0");
            Span t = new Span(e[1]);
            t.getStyle().set("font-size", "0.75rem").set("font-weight", "600").set("color", "#553722")
                    .set("font-family", "'Plus Jakarta Sans', sans-serif");
            item.add(p, t);
            legende.add(item);
        }
        return legende;
    }

    /**
     * Erstellt einen einzelnen gruppierten Balken (Bar + Karte) für das Stundendiagramm.
     *
     * @param label  Stunden-Label (z.B. "8", "9", ...)
     * @param barH   CSS-Höhe des Barzahlungs-Balkens (z.B. "60%")
     * @param karteH CSS-Höhe des Kartenzahlungs-Balkens
     * @return vertikales Layout mit zwei Balken und Label
     */
    static VerticalLayout buildBalken(String label, String barH, String karteH) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.getStyle()
                .set("flex", "1").set("gap", "0.35rem").set("height", "100%").set("justify-content", "flex-end");

        HorizontalLayout paar = new HorizontalLayout();
        paar.setAlignItems(FlexComponent.Alignment.END);
        paar.setSpacing(false);
        paar.getStyle().set("gap", "0.15rem").set("width", "100%").set("height", "calc(100% - 1.25rem)");

        Div bar = new Div();
        bar.getStyle().set("flex", "1").set("height", barH)
                .set("background", "#ffdcc6").set("border-radius", "0.3rem 0.3rem 0 0");
        Div karte = new Div();
        karte.getStyle().set("flex", "1").set("height", karteH)
                .set("background", "#553722").set("border-radius", "0.3rem 0.3rem 0 0");
        paar.add(bar, karte);

        Span lbl = new Span(label);
        lbl.getStyle().set("font-size", "0.5rem").set("font-weight", "700").set("color", "#82746d")
                .set("font-family", "'Plus Jakarta Sans', sans-serif")
                .set("text-align", "center").set("white-space", "nowrap");
        wrapper.add(paar, lbl);
        return wrapper;
    }

    /**
     * Erstellt den dynamischen Legenden-Block mit Punkt, Name, Betrag und Prozentanteil.
     */
    private static Div buildLegendeBlock(String[][] daten, double summe) {
        Div legende = new Div();
        legende.getStyle()
                .set("display", "flex").set("flex-direction", "column").set("gap", "0.75rem")
                .set("flex-shrink", "0").set("min-width", "120px");

        for (String[] d : daten) {
            double wert = Double.parseDouble(d[1]);
            int p = (int) Math.round(wert / summe * 100);

            Div punkt = new Div();
            punkt.getStyle().set("width", "10px").set("height", "10px").set("border-radius", "50%")
                    .set("background", d[2]).set("flex-shrink", "0");
            Span nm = new Span(d[0]);
            nm.getStyle().set("font-size", "0.8rem").set("font-weight", "700").set("color", "#553722")
                    .set("font-family", "'Plus Jakarta Sans', sans-serif");
            Div lz = new Div();
            lz.getStyle().set("display", "flex").set("align-items", "center").set("gap", "0.5rem");
            lz.add(punkt, nm);

            Span inf = new Span(String.format("%.0f€ · %d%%", wert, p));
            inf.getStyle().set("font-size", "0.7rem").set("color", "#82746d")
                    .set("font-family", "'Plus Jakarta Sans', sans-serif").set("padding-left", "1.25rem");

            Div eintrag = new Div();
            eintrag.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "0.15rem");
            eintrag.add(lz, inf);
            legende.add(eintrag);
        }
        return legende;
    }

    /**
     * Erstellt das Canvas-Element und führt das Zeichenskript per JavaScript aus.
     */
    private static Div buildCanvasWidget(String[][] daten, boolean balken) {
        Div cw = new Div();
        cw.getStyle().set("flex", "1").set("display", "flex").set("justify-content", "center");
        String cid = "chart-" + System.nanoTime();
        cw.getElement().setProperty("innerHTML",
                "<canvas id='" + cid + "' width='200' height='200' style='display:block;'></canvas>");
        String js = balken ? buildBalkenJs(cid, daten) : buildTorteJs(cid, daten);
        cw.getElement().executeJs("requestAnimationFrame(function() { setTimeout(function() { " + js + " }, 100); });");
        return cw;
    }

    /**
     * Generiert den JavaScript-Code zum Zeichnen eines Balkendiagramms auf dem Canvas.
     *
     * @param cid   Canvas-Element-ID
     * @param daten Diagrammdaten
     * @return JavaScript als String
     */
    private static String buildBalkenJs(String cid, String[][] daten) {
        StringBuilder js = new StringBuilder();
        js.append("var c=document.getElementById('").append(cid).append("');if(!c)return;");
        js.append("var ctx=c.getContext('2d');var w=c.width,h=c.height;ctx.clearRect(0,0,w,h);");
        js.append("var labels=["); for(String[]d:daten)js.append("'").append(d[0]).append("',"); js.append("];");
        js.append("var werte=[");  for(String[]d:daten)js.append(d[1]).append(",");              js.append("];");
        js.append("var farben=["); for(String[]d:daten)js.append("'").append(d[2]).append("',"); js.append("];");
        js.append("var maxW=Math.max.apply(null,werte)||1;var n=werte.length;");
        js.append("var padT=20,padB=25,padL=10,padR=10;");
        js.append("var chartH=h-padT-padB;var chartW=w-padL-padR;var slot=chartW/n;var bw=slot*0.5;");
        js.append("for(var i=0;i<n;i++){");
        js.append("var bh=(werte[i]/maxW)*chartH;var x=padL+slot*i+(slot-bw)/2;var y=padT+chartH-bh;");
        js.append("ctx.fillStyle=farben[i];var r=6;ctx.beginPath();");
        js.append("ctx.moveTo(x+r,y);ctx.lineTo(x+bw-r,y);ctx.quadraticCurveTo(x+bw,y,x+bw,y+r);");
        js.append("ctx.lineTo(x+bw,y+bh);ctx.lineTo(x,y+bh);ctx.lineTo(x,y+r);");
        js.append("ctx.quadraticCurveTo(x,y,x+r,y);ctx.closePath();ctx.fill();");
        js.append("ctx.fillStyle='#553722';ctx.font='bold 10px sans-serif';ctx.textAlign='center';");
        js.append("ctx.fillText(Math.round(werte[i])+'€',x+bw/2,y-5);");
        js.append("ctx.fillStyle='#82746d';ctx.font='9px sans-serif';");
        js.append("ctx.fillText(labels[i].toUpperCase(),x+bw/2,h-8);}");
        return js.toString();
    }

    /**
     * Generiert den JavaScript-Code zum Zeichnen eines Tortendiagramms auf dem Canvas.
     *
     * @param cid   Canvas-Element-ID
     * @param daten Diagrammdaten
     * @return JavaScript als String
     */
    private static String buildTorteJs(String cid, String[][] daten) {
        StringBuilder js = new StringBuilder();
        js.append("var c=document.getElementById('").append(cid).append("');if(!c)return;");
        js.append("var ctx=c.getContext('2d');var w=c.width,h=c.height;ctx.clearRect(0,0,w,h);");
        js.append("var werte=[");  for(String[]d:daten)js.append(d[1]).append(","); js.append("];");
        js.append("var farben=["); for(String[]d:daten)js.append("'").append(d[2]).append("',"); js.append("];");
        js.append("var s=werte.reduce(function(a,b){return a+b;},0)||1;");
        js.append("var cx=w/2,cy=h/2,r=Math.min(w,h)/2-10,start=-Math.PI/2;");
        js.append("for(var i=0;i<werte.length;i++){");
        js.append("var sl=(werte[i]/s)*2*Math.PI;ctx.fillStyle=farben[i];ctx.beginPath();");
        js.append("ctx.moveTo(cx,cy);ctx.arc(cx,cy,r,start,start+sl);ctx.closePath();ctx.fill();");
        js.append("ctx.strokeStyle='white';ctx.lineWidth=2;ctx.stroke();start+=sl;}");
        return js.toString();
    }
}