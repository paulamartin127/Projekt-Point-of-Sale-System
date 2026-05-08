package de.fhswf.kassensystem.views.berichte;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.fhswf.kassensystem.model.dto.ArtikelStatistikDTO;

import java.util.Base64;
import java.util.List;

/**
 * Panel für den "Artikelstatistik"-Tab in der Berichte-View.
 *
 * <p>Zeigt ein Verkaufsranking der letzten 30 Tage als Liste mit
 * horizontalen Fortschrittsbalken. Der Artikel mit der höchsten Verkaufszahl
 * dient als Referenzwert (100 %) für alle anderen Balken.
 *
 * @author Adrian Krawietz
 */
class ArtikelstatistikPanel extends VerticalLayout {

    /**
     * Erstellt das Panel und rendert das Verkaufsranking.
     *
     * @param statistiken Liste der Artikel-Statistiken, absteigend nach Verkaufsmenge sortiert
     */
    ArtikelstatistikPanel(List<ArtikelStatistikDTO> statistiken) {
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "2rem");

        VerticalLayout karte = new VerticalLayout();
        karte.setWidthFull();
        karte.setPadding(false);
        karte.setSpacing(false);
        karte.getStyle().set("background", "white").set("border-radius", "1.25rem")
                .set("padding", "2rem").set("gap", "1.5rem");

        H3 titel = new H3("Artikelstatistik – Verkaufsranking (letzte 30 Tage)");
        titel.getStyle().set("margin", "0").set("font-size", "1.1rem").set("font-weight", "700")
                .set("color", "#553722").set("font-family", "'Plus Jakarta Sans', sans-serif");

        VerticalLayout liste = new VerticalLayout();
        liste.setWidthFull();
        liste.setPadding(false);
        liste.setSpacing(false);
        liste.getStyle().set("gap", "1rem");

        int maxAnzahl = statistiken.isEmpty() ? 1 : statistiken.getFirst().getAnzahlVerkauft();
        for (ArtikelStatistikDTO dto : statistiken) {
            liste.add(buildStatistikZeile(dto.getArtikel().getName(),
                    dto.getArtikel().getKategorie().getName(),
                    dto.getAnzahlVerkauft() + "x",
                    dto.getAnzahlVerkauft(), maxAnzahl,
                    dto.getArtikel().getBild()));
        }
        if (statistiken.isEmpty()) liste.add(BerichteUtils.leerSpan("Keine Verkaufsdaten der letzten 30 Tage."));

        karte.add(titel, liste);
        add(karte);
    }

    /**
     * Erstellt eine einzelne Ranking-Zeile mit Artikelname, Kategorie, Anzahl und Fortschrittsbalken.
     *
     * @param name      Artikelname
     * @param kat       Kategoriename
     * @param anzahl    formatierte Verkaufsanzahl (z.B. "42x")
     * @param anzahlInt numerischer Wert für die Balkenbreite
     * @param maxAnzahl Maximalwert (= 100 % Balkenbreite)
     * @param bild Artikelbild als byte-Array oder Fallback-Icon
     */
    private VerticalLayout buildStatistikZeile(String name, String kat,
                                               String anzahl, int anzahlInt, int maxAnzahl, byte[] bild) {
        HorizontalLayout kopf = new HorizontalLayout();
        kopf.setWidthFull();
        kopf.setAlignItems(FlexComponent.Alignment.CENTER);
        kopf.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        kopf.setPadding(false);

        Div av = new Div();
        av.getStyle().set("width", "2.25rem").set("height", "2.25rem").set("border-radius", "9999px")
                .set("background", "#efecff").set("flex-shrink", "0")
                .set("overflow", "hidden").set("display", "flex")
                .set("align-items", "center").set("justify-content", "center");

        if (bild != null && bild.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(bild);
            Image img = new Image("data:image/jpeg;base64," + base64, name);
            img.getStyle().set("width", "100%").set("height", "100%").set("object-fit", "cover");
            av.add(img);
        } else {
            Span icon = new Span("lunch_dining");
            icon.addClassName("material-symbols-outlined");
            icon.getStyle().set("font-size", "1.1rem").set("color", "#82746d");
            av.add(icon);
        }

        Span n = new Span(name);
        n.getStyle().set("font-weight", "700").set("font-size", "0.875rem").set("color", "#553722")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");
        Span k = new Span(kat);
        k.getStyle().set("font-size", "0.7rem").set("color", "#82746d")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.add(n, k);
        HorizontalLayout links = new HorizontalLayout();
        links.setAlignItems(FlexComponent.Alignment.CENTER);
        links.setSpacing(false);
        links.getStyle().set("gap", "0.75rem");
        links.add(av, info);

        Span a = new Span(anzahl);
        a.getStyle().set("font-weight", "900").set("font-size", "0.875rem").set("color", "#553722")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");
        kopf.add(links, a);

        int pct = maxAnzahl == 0 ? 0 : (int)((anzahlInt / (double) maxAnzahl) * 100);
        Div fill = new Div();
        fill.getStyle().set("width", pct + "%").set("height", "100%")
                .set("background", "#553722").set("border-radius", "9999px");
        Div bg = new Div(fill);
        bg.getStyle().set("width", "100%").set("height", "0.3rem")
                .set("background", "#efecff").set("border-radius", "9999px");

        VerticalLayout zeile = new VerticalLayout();
        zeile.setWidthFull();
        zeile.setPadding(false);
        zeile.setSpacing(false);
        zeile.getStyle().set("gap", "0.4rem");
        zeile.add(kopf, bg);
        return zeile;
    }
}
