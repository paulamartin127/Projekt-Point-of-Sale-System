package de.fhswf.kassensystem.views.berichte;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.fhswf.kassensystem.model.Artikel;
import de.fhswf.kassensystem.model.Verkauf;
import de.fhswf.kassensystem.model.Verkaufsposition;
import de.fhswf.kassensystem.model.enums.Zahlungsart;
import de.fhswf.kassensystem.exception.KassensystemException;
import de.fhswf.kassensystem.service.BerichteService;
import de.fhswf.kassensystem.views.components.FehlerUI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Panel für den "Umsatzübersicht"-Tab in der Berichte-View.
 *
 * <p>Zeigt wahlweise die Tages- oder Wochenansicht. Die Tagesansicht enthält
 * ein Stundendiagramm (8–18 Uhr, Bar/Karte) und eine Produktliste.
 * Die Wochenansicht zeigt ein Wochendiagramm (Mo–Sa) und eine Zusammenfassung.
 *
 * @author Adrian Krawietz
 */
class UmsatzuebersichtPanel extends VerticalLayout {

    private final BerichteService service;
    private final LocalDate       aktivDatum;
    private final Div             umsatzContainer = new Div();

    /**
     * Erstellt das Panel mit Tagesansicht als Standard.
     *
     * @param service     Service zum Laden der Verkaufsdaten
     * @param aktivDatum  das aktuell gewählte Datum in der Berichte-View
     */
    UmsatzuebersichtPanel(BerichteService service, LocalDate aktivDatum) {
        this.service    = service;
        this.aktivDatum = aktivDatum;
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "2rem");
        umsatzContainer.setWidthFull();
        umsatzContainer.add(buildTagesansicht());
        add(buildKopfZeile(), umsatzContainer);
    }

    /**
     * Erstellt den Kopfbereich mit Titel und Tag/Woche-Toggle-Buttons.
     * Ein Klick auf einen Toggle-Button lädt den entsprechenden Inhalt neu.
     */
    private HorizontalLayout buildKopfZeile() {
        Button tagBtn   = DiagrammFactory.buildToggleButton("Tag",   true);
        Button wocheBtn = DiagrammFactory.buildToggleButton("Woche", false);
        tagBtn.addClickListener(e -> {
            umsatzContainer.removeAll();
            umsatzContainer.add(buildTagesansicht());
            tagBtn.getStyle().set("background", "#553722").set("color", "white");
            wocheBtn.getStyle().set("background", "transparent").set("color", "#553722");
        });
        wocheBtn.addClickListener(e -> {
            umsatzContainer.removeAll();
            umsatzContainer.add(buildWochenansicht());
            wocheBtn.getStyle().set("background", "#553722").set("color", "white");
            tagBtn.getStyle().set("background", "transparent").set("color", "#553722");
        });

        HorizontalLayout toggle = new HorizontalLayout();
        toggle.setSpacing(false);
        toggle.getStyle().set("background", "#efecff").set("border-radius", "9999px")
                .set("padding", "0.25rem").set("gap", "0.25rem");
        toggle.add(tagBtn, wocheBtn);

        H3 titel = new H3("Umsatzübersicht");
        titel.getStyle().set("margin", "0").set("font-size", "1.25rem").set("font-weight", "800")
                .set("color", "#553722").set("font-family", "'Plus Jakarta Sans', sans-serif");

        HorizontalLayout kopf = new HorizontalLayout();
        kopf.setWidthFull();
        kopf.setAlignItems(FlexComponent.Alignment.CENTER);
        kopf.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        kopf.setPadding(false);
        kopf.add(titel, toggle);
        return kopf;
    }

    /**
     * Erstellt die Tagesansicht mit Stundendiagramm und Produktliste.
     * Bei einem Service-Fehler wird ein Fallback-Layout angezeigt.
     */
    private VerticalLayout buildTagesansicht() {
        try {
            List<Verkauf> verkaeufe = service.findByTimestampBetween(
                    aktivDatum.atStartOfDay(), aktivDatum.atTime(23, 59, 59));

            BigDecimal[] bar   = new BigDecimal[11];
            BigDecimal[] karte = new BigDecimal[11];
            BigDecimal   maxH  = BigDecimal.ONE;
            for (int i = 0; i < 11; i++) { bar[i] = BigDecimal.ZERO; karte[i] = BigDecimal.ZERO; }
            for (Verkauf v : verkaeufe) {
                int h = v.getTimestamp().getHour();
                if (h >= 8 && h <= 18) {
                    int idx = h - 8;
                    BigDecimal s = BerichteUtils.safe(v.getGesamtsumme());
                    if (v.getZahlungsart() == Zahlungsart.BAR) bar[idx]   = bar[idx].add(s);
                    else                                        karte[idx] = karte[idx].add(s);
                    BigDecimal tot = bar[idx].add(karte[idx]);
                    if (tot.compareTo(maxH) > 0) maxH = tot;
                }
            }

            HorizontalLayout stundenDiag = new HorizontalLayout();
            stundenDiag.setWidthFull();
            stundenDiag.setAlignItems(FlexComponent.Alignment.END);
            stundenDiag.setSpacing(false);
            stundenDiag.getStyle().set("height", "14rem").set("gap", "0.5rem").set("padding", "0 0.5rem");
            String[] labels = {"8:00-8:59","9:00-9:59","10:00-10:59","11:00-11:59","12:00-12:59","13:00-13:59","14:00-14:59","15:00-15:59","16:00-16:59","17:00-17:59","18:00-18:59"};
            for (int i = 0; i < 11; i++) {
                stundenDiag.add(DiagrammFactory.buildBalken(labels[i],
                        Math.max(BerichteUtils.pct(bar[i], maxH), 2) + "%",
                        Math.max(BerichteUtils.pct(karte[i], maxH), 2) + "%"));
            }

            VerticalLayout stundenKarte = new VerticalLayout();
            stundenKarte.setWidthFull();
            stundenKarte.setPadding(false);
            stundenKarte.setSpacing(false);
            stundenKarte.getStyle().set("background", "white").set("border-radius", "1.25rem")
                    .set("padding", "2rem").set("gap", "1.25rem");

            H3 stundenTitel = new H3("Umsatz nach Stunde – " +
                    aktivDatum.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            stundenTitel.getStyle().set("margin", "0").set("font-size", "1.1rem").set("font-weight", "700")
                    .set("color", "#553722").set("font-family", "'Plus Jakarta Sans', sans-serif");
            stundenKarte.add(stundenTitel, stundenDiag, DiagrammFactory.buildLegende());

            VerticalLayout layout = new VerticalLayout();
            layout.setWidthFull();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.getStyle().set("gap", "1.5rem");
            layout.add(stundenKarte, buildProduktListe(verkaeufe));
            return layout;
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
            return buildFehlerLayout("Tagesansicht konnte nicht geladen werden.");
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
            return buildFehlerLayout("Tagesansicht konnte nicht geladen werden.");
        }
    }

    /**
     * Aggregiert alle Verkaufspositionen des Tages nach Artikel und rendert die Liste.
     *
     * @param verkaeufe alle Verkäufe des aktiven Datums
     * @return Karte mit Produktliste
     */
    private VerticalLayout buildProduktListe(List<Verkauf> verkaeufe) {
        Map<Artikel, int[]> stats = new LinkedHashMap<>();
        for (Verkauf v : verkaeufe) {
            if (v.getPositionen() == null) continue;
            for (Verkaufsposition pos : v.getPositionen()) {
                stats.computeIfAbsent(pos.getArtikel(), k -> new int[]{0, 0});
                stats.get(pos.getArtikel())[0] += pos.getMenge();
                stats.get(pos.getArtikel())[1] +=
                        BerichteUtils.safe(pos.getEinzelpreis())
                                .multiply(BigDecimal.valueOf(pos.getMenge()))
                                .multiply(BigDecimal.valueOf(100)).intValue();
            }
        }

        VerticalLayout liste = new VerticalLayout();
        liste.setWidthFull();
        liste.setPadding(false);
        liste.setSpacing(false);
        liste.getStyle().set("gap", "0.75rem");
        stats.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .forEach(e -> {
                    BigDecimal u = BigDecimal.valueOf(e.getValue()[1])
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    liste.add(ProduktZeileFactory.create(
                            e.getKey().getName(), e.getKey().getKategorie().getName(),
                            e.getValue()[0] + "x", BerichteUtils.fp(u),
                            e.getKey().getBild()));
                });
        if (stats.isEmpty()) liste.add(BerichteUtils.leerSpan("Keine Verkäufe an diesem Tag."));

        VerticalLayout karte = new VerticalLayout();
        karte.setWidthFull();
        karte.setPadding(false);
        karte.setSpacing(false);
        karte.getStyle().set("background", "white").set("border-radius", "1.25rem")
                .set("padding", "2rem").set("gap", "1.25rem");
        H3 pTitel = new H3("Verkaufte Produkte – " +
                aktivDatum.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        pTitel.getStyle().set("margin", "0").set("font-size", "1.1rem").set("font-weight", "700")
                .set("color", "#553722").set("font-family", "'Plus Jakarta Sans', sans-serif");
        karte.add(pTitel, liste);
        return karte;
    }

    /**
     * Erstellt die Wochenansicht mit tageweisem Balkendiagramm (Mo–Sa)
     * und einer Zusammenfassung (Wochenumsatz, Transaktionen, stärkster Tag).
     * Bei einem Service-Fehler wird ein Fallback-Layout angezeigt.
     */
    private VerticalLayout buildWochenansicht() {
        try {
            LocalDate wochenStart = aktivDatum.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            String[]   tagLabels  = {"MO","DI","MI","DO","FR","SA"};
            BigDecimal[] barW     = new BigDecimal[6];
            BigDecimal[] karteW   = new BigDecimal[6];
            BigDecimal   maxTag   = BigDecimal.ONE;
            BigDecimal   wUmsatz  = BigDecimal.ZERO;
            int          wTrans   = 0;

            for (int i = 0; i < 6; i++) {
                barW[i] = BigDecimal.ZERO; karteW[i] = BigDecimal.ZERO;
                List<Verkauf> tv = service.findByTimestampBetween(
                        wochenStart.plusDays(i).atStartOfDay(),
                        wochenStart.plusDays(i).atTime(23, 59, 59));
                wTrans += tv.size();
                for (Verkauf v : tv) {
                    BigDecimal s = BerichteUtils.safe(v.getGesamtsumme());
                    wUmsatz = wUmsatz.add(s);
                    if (v.getZahlungsart() == Zahlungsart.BAR) barW[i] = barW[i].add(s);
                    else                                        karteW[i] = karteW[i].add(s);
                }
                BigDecimal tot = barW[i].add(karteW[i]);
                if (tot.compareTo(maxTag) > 0) maxTag = tot;
            }

            HorizontalLayout wDiag = new HorizontalLayout();
            wDiag.setWidthFull();
            wDiag.setAlignItems(FlexComponent.Alignment.END);
            wDiag.setSpacing(false);
            wDiag.getStyle().set("height", "16rem").set("gap", "1rem").set("padding", "0 1rem");

            BigDecimal starkWert = BigDecimal.ZERO;
            String     starkTag  = "-";
            for (int i = 0; i < 6; i++) {
                int bp = Math.max(BerichteUtils.pct(barW[i], maxTag), 2);
                int kp = Math.max(BerichteUtils.pct(karteW[i], maxTag), 2);
                wDiag.add(DiagrammFactory.buildBalken(tagLabels[i], bp + "%", kp + "%"));
                BigDecimal sum = barW[i].add(karteW[i]);
                if (sum.compareTo(starkWert) > 0) { starkWert = sum; starkTag = tagLabels[i]; }
            }

            VerticalLayout karte = new VerticalLayout();
            karte.setWidthFull();
            karte.setPadding(false);
            karte.setSpacing(false);
            karte.getStyle().set("background", "white").set("border-radius", "1.25rem")
                    .set("padding", "2rem").set("gap", "1.25rem");

            H3 wTitel = new H3("Umsatz nach Zahlungsart – KW " +
                    aktivDatum.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + " (Mo–Sa)");
            wTitel.getStyle().set("margin", "0").set("font-size", "1.1rem").set("font-weight", "700")
                    .set("color", "#553722").set("font-family", "'Plus Jakarta Sans', sans-serif");

            karte.add(wTitel, wDiag, DiagrammFactory.buildLegende(),
                    buildWochenSummary(wUmsatz, wTrans, starkTag));

            VerticalLayout layout = new VerticalLayout();
            layout.setWidthFull();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.getStyle().set("gap", "1.5rem");
            layout.add(karte);
            return layout;
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
            return buildFehlerLayout("Wochenansicht konnte nicht geladen werden.");
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
            return buildFehlerLayout("Wochenansicht konnte nicht geladen werden.");
        }
    }

    /**
     * Erstellt die drei Zusammenfassungs-Kacheln unterhalb des Wochendiagramms.
     *
     * @param wUmsatz  Gesamtumsatz der Woche
     * @param wTrans   Anzahl Transaktionen der Woche
     * @param starkTag Kürzel des umsatzstärksten Tags (z.B. "FR")
     */
    private HorizontalLayout buildWochenSummary(BigDecimal wUmsatz, int wTrans, String starkTag) {
        HorizontalLayout summary = new HorizontalLayout();
        summary.setWidthFull();
        summary.setSpacing(false);
        summary.getStyle().set("gap", "1rem");
        for (String[] s : new String[][]{
                {"Wochenumsatz",  BerichteUtils.fp(wUmsatz), "payments"},
                {"Transaktionen", String.valueOf(wTrans),     "receipt_long"},
                {"Stärkster Tag", starkTag,                   "star"}}) {
            VerticalLayout k = new VerticalLayout();
            k.setPadding(false);
            k.setSpacing(false);
            k.getStyle().set("flex", "1").set("background", "#f5f2ff")
                    .set("border-radius", "1rem").set("padding", "1rem 1.25rem").set("gap", "0.25rem");
            Span l = new Span(s[0].toUpperCase());
            l.getStyle().set("font-size", "0.6rem").set("font-weight", "800")
                    .set("letter-spacing", "0.1em").set("color", "#82746d")
                    .set("font-family", "'Plus Jakarta Sans', sans-serif");
            Span ic = new Span(s[2]);
            ic.addClassName("material-symbols-outlined");
            ic.getStyle().set("font-size", "1rem").set("color", "#553722").set("line-height", "1");
            Span w = new Span(s[1]);
            w.getStyle().set("font-size", "1.1rem").set("font-weight", "900").set("color", "#553722")
                    .set("letter-spacing", "-0.025em").set("font-family", "'Plus Jakarta Sans', sans-serif");
            HorizontalLayout wz = new HorizontalLayout();
            wz.setAlignItems(FlexComponent.Alignment.CENTER);
            wz.setSpacing(false);
            wz.getStyle().set("gap", "0.5rem");
            wz.add(ic, w);
            k.add(l, wz);
            summary.add(k);
        }
        return summary;
    }

    /**
     * Erstellt ein Fallback-Layout wenn Daten nicht geladen werden konnten.
     * Wird anstelle des Diagramms angezeigt.
     *
     * @param nachricht benutzerfreundliche Fehlermeldung
     */
    private VerticalLayout buildFehlerLayout(String nachricht) {
        Span icon = new Span("error_outline");
        icon.addClassName("material-symbols-outlined");
        icon.getStyle().set("font-size", "2rem").set("color", "#ba1a1a");

        Span text = new Span(nachricht);
        text.getStyle()
                .set("font-size", "0.9rem").set("color", "#ba1a1a")
                .set("font-family", "'Plus Jakarta Sans', sans-serif");

        VerticalLayout fehler = new VerticalLayout(icon, text);
        fehler.setWidthFull();
        fehler.setAlignItems(FlexComponent.Alignment.CENTER);
        fehler.getStyle()
                .set("background", "#fff0f0").set("border-radius", "1.25rem")
                .set("padding", "3rem 2rem").set("gap", "0.75rem");
        return fehler;
    }
}