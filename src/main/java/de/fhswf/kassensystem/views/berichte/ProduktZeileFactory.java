package de.fhswf.kassensystem.views.berichte;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Base64;

/**
 * Fabrikklasse für einzelne Produkt-Zeilen in der Umsatzübersicht.
 *
 * <p>Jede Zeile zeigt Artikelbild, Artikelname, Kategorie,
 * verkaufte Menge und den Gesamtumsatz des Artikels.
 *
 * @author Adrian Krawietz
 */
class ProduktZeileFactory {

    private ProduktZeileFactory() {}

    /**
     * Erstellt eine Produktzeile mit Avatar, Name/Kategorie, Menge und Umsatz.
     *
     * @param name   Artikelname
     * @param kat    Kategoriename
     * @param menge  formatierte Menge (z.B. "12x")
     * @param umsatz formatierter Umsatzbetrag (z.B. "14,88€")
     * @param bild Artikelbild als byte-Array oder Fallback-Icon
     * @return fertig gestyltes Zeilen-Layout
     */
    static HorizontalLayout create(String name, String kat, String menge, String umsatz, byte[] bild) {
        Div av = new Div();
        av.getStyle().set("width", "2rem").set("height", "2rem").set("border-radius", "9999px")
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
            icon.getStyle().set("font-size", "1rem").set("color", "#82746d");
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

        Span m = new Span(menge);
        m.getStyle().set("font-size", "0.8rem").set("font-weight", "700").set("color", "#82746d")
                .set("min-width", "3rem").set("text-align", "right")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");
        Span u = new Span(umsatz);
        u.getStyle().set("font-size", "0.875rem").set("font-weight", "900").set("color", "#553722")
                .set("min-width", "5rem").set("text-align", "right")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");

        HorizontalLayout rechts = new HorizontalLayout();
        rechts.setAlignItems(FlexComponent.Alignment.CENTER);
        rechts.setSpacing(false);
        rechts.getStyle().set("gap", "2rem");
        rechts.add(m, u);

        HorizontalLayout zeile = new HorizontalLayout();
        zeile.setWidthFull();
        zeile.setAlignItems(FlexComponent.Alignment.CENTER);
        zeile.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        zeile.setPadding(false);
        zeile.getStyle().set("padding", "0.5rem 0").set("border-bottom", "1px solid #f5f2ff");
        zeile.add(links, rechts);
        return zeile;
    }
}