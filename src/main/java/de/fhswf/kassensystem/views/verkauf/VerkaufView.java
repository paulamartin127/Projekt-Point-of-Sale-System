package de.fhswf.kassensystem.views.verkauf;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import de.fhswf.kassensystem.broadcast.Broadcaster;
import de.fhswf.kassensystem.exception.KassensystemException;
import de.fhswf.kassensystem.model.Artikel;
import de.fhswf.kassensystem.model.Verkauf;
import de.fhswf.kassensystem.model.Verkaufsposition;
import de.fhswf.kassensystem.model.enums.Zahlungsart;
import de.fhswf.kassensystem.service.ArtikelService;
import de.fhswf.kassensystem.service.PdfExportService;
import de.fhswf.kassensystem.service.VerkaufService;
import de.fhswf.kassensystem.views.MainLayout;
import de.fhswf.kassensystem.views.components.FehlerUI;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kassier-View (Point of Sale) – Hauptansicht für den Verkaufsvorgang.
 *
 * <p>Aufbau: linke Spalte mit Artikelgrid (Suche, Kategoriefilter, Karten),
 * rechte Spalte mit Warenkorb (Positionen, Rabatt, Bezahlen).
 *
 * <p>Der Bestandsbadge jeder Artikelkarte wird live per JavaScript aktualisiert
 * wenn ein Artikel in den Warenkorb gelegt oder daraus entfernt wird.
 * Eine {@code kartenMap} (artikelId → Div) ermöglicht gezieltes Badge-Update
 * ohne die gesamte Karte neu zu rendern.
 *
 * <p>Zugriff: Rollen {@code KASSIERER} und {@code MANAGER}.
 *
 * @author Adrian Krawietz
 */
@RolesAllowed({"KASSIERER", "MANAGER"})
@Route(value = "kassieren", layout = MainLayout.class)
public class VerkaufView extends HorizontalLayout implements BeforeEnterObserver {

    private final ArtikelService           artikelService;
    private final VerkaufService           verkaufService;
    private final PdfExportService         pdfExportService;
    private final WarenkorbZusammenfassung zusammenfassung;
    private Long letzterVerkaufId = null;

    private final List<WarenkorbEintrag>  warenkorbListe  = new ArrayList<>();
    private final Map<Long, Div>          kartenMap       = new HashMap<>();
    private String aktiveKategorie = "Alle";
    private String aktuelleSuche   = "";

    private final VerticalLayout warenkorbPositionenLayout = new VerticalLayout();
    private final Div            artikelGridDiv            = new Div();

    private Registration broadcasterRegistration;

    /**
     * Erstellt die View mit linker Artikel-Spalte und rechter Warenkorb-Spalte.
     *
     * @param artikelService   Service für Artikel-Abfragen
     * @param verkaufService   Service für das Abschließen von Verkäufen
     * @param pdfExportService Service für den Kassenbon-PDF-Export
     */
    public VerkaufView(ArtikelService artikelService, VerkaufService verkaufService,
                       PdfExportService pdfExportService) {
        this.artikelService   = artikelService;
        this.verkaufService   = verkaufService;
        this.pdfExportService = pdfExportService;
        this.zusammenfassung  = new WarenkorbZusammenfassung(
                this::warenkorbLeeren,
                gesamtBetrag -> {
                    if (warenkorbListe.isEmpty()) {
                        FehlerUI.fehler("Warenkorb ist leer.");
                        return;
                    }
                    String betrag = gesamtBetrag != null ? gesamtBetrag : "0,00€";
                    new ZahlungsDialog(betrag, this::verkaufAbschliessen).open();
                },
                this::aktualisiereWarenkorbUI
        );
        zusammenfassung.getElement().setAttribute("tour-id", "zusammenfassung");

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("overflow", "hidden").set("height", "100vh");

        warenkorbPositionenLayout.setPadding(false);
        warenkorbPositionenLayout.setSpacing(false);
        warenkorbPositionenLayout.getStyle()
                .set("padding", "0 1.5rem").set("gap", "0.25rem")
                .set("flex", "1").set("overflow-y", "auto");

        add(buildArtikelSpalte(), buildWarenkorbSpalte());
    }

    // ═══════════════════════════════════════════════════════════
    // LINKE SPALTE
    // ═══════════════════════════════════════════════════════════

    /**
     * Erstellt die linke Spalte mit Suchfeld, Kategorie-Chips und Artikel-Grid.
     * Das Grid passt sich via ResizeObserver auf 2 oder 3 Spalten an.
     */
    private VerticalLayout buildArtikelSpalte() {
        VerticalLayout spalte = new VerticalLayout();
        spalte.setPadding(false);
        spalte.setSpacing(false);
        spalte.getStyle()
                .set("flex", "0 0 60%").set("max-width", "55%")
                .set("background", "#fcf8ff").set("overflow-y", "auto")
                .set("height", "100%").set("padding", "2rem").set("box-sizing", "border-box");

        artikelGridDiv.addClassName("artikel-grid");
        artikelGridDiv.getElement().setAttribute("tour-id", "artikel-grid");
        artikelGridDiv.getStyle()
                .set("width", "100%").set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)").set("gap", "1.25rem");

        ladeArtikelGrid();
        spalte.add(buildSuchfeld(), buildKategorieFilter(), artikelGridDiv);

        spalte.getElement().executeJs(
                "const observer = new ResizeObserver(entries => {" +
                        "  for (const entry of entries) {" +
                        "    const w = entry.contentRect.width;" +
                        "    const grid = this.querySelector('.artikel-grid');" +
                        "    if (!grid) return;" +
                        "    grid.style.gridTemplateColumns = w < 600 ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)';" +
                        "  }" +
                        "});" +
                        "observer.observe(this);"
        );
        return spalte;
    }

    /**
     * Baut das Artikel-Grid neu auf – gefiltert nach aktiver Kategorie und Suchbegriff.
     * Berechnet für jede Karte den aktuellen Anzeigebestand (DB-Bestand minus Warenkorbmenge).
     */
    private void ladeArtikelGrid() {
        artikelGridDiv.removeAll();
        kartenMap.clear();
        try {
            List<Artikel> alle = (aktuelleSuche.isBlank()
                    ? artikelService.findAllArtikel()
                    : artikelService.findByName(aktuelleSuche))
                    .stream()
                    .sorted(Comparator.comparing(Artikel::getName))
                    .toList();

            for (Artikel a : alle) {
                if (!aktiveKategorie.equals("Alle") && !a.getKategorie().getName().equals(aktiveKategorie)) continue;
                if (!a.isAktiv()) continue;

                int imKorb = warenkorbListe.stream()
                        .filter(e -> e.artikel.getId().equals(a.getId()))
                        .mapToInt(e -> e.menge)
                        .sum();
                int anzeigeBestand = a.getBestand() >= 999 ? 999 : Math.max(0, a.getBestand() - imKorb);
                boolean ausverkauft = a.getBestand() == 0;

                Div karte = ArtikelKarteFactory.create(a, anzeigeBestand, ausverkauft,
                        this::artikelZumKorbHinzufuegen);
                kartenMap.put(a.getId(), karte);
                artikelGridDiv.add(karte);
            }
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
        }
    }

    /**
     * Erstellt die Suchfeld-Zeile über dem Artikel-Grid.
     * Jede Eingabe filtert das Grid live.
     */
    private HorizontalLayout buildSuchfeld() {
        TextField search = new TextField();
        search.setWidthFull();
        search.setPlaceholder("Artikel suchen...");
        search.setPrefixComponent(createIcon("search"));
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> { aktuelleSuche = e.getValue(); ladeArtikelGrid(); });

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("margin-bottom", "1.5rem");
        row.getElement().setAttribute("tour-id", "artikel-suche");
        row.add(search);
        return row;
    }

    /**
     * Erstellt die Kategorie-Chip-Gruppe aus allen aktiven Artikelkategorien.
     */
    private KategorieChipGroup buildKategorieFilter() {
        List<String> kategorien = new ArrayList<>();
        try {
            kategorien = artikelService.findAllArtikel().stream()
                    .filter(Artikel::isAktiv)
                    .map(a -> a.getKategorie().getName())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
        }
        KategorieChipGroup chipGroup = new KategorieChipGroup(kategorien, kat -> {
            aktiveKategorie = kat;
            ladeArtikelGrid();
        });
        chipGroup.getElement().setAttribute("tour-id", "kategorie-chips");
        return chipGroup;
    }

    // ═══════════════════════════════════════════════════════════
    // RECHTE SPALTE
    // ═══════════════════════════════════════════════════════════

    /**
     * Erstellt die rechte Warenkorb-Spalte mit Header, Positionen und Zusammenfassung.
     */
    private VerticalLayout buildWarenkorbSpalte() {
        VerticalLayout spalte = new VerticalLayout();
        spalte.setPadding(false);
        spalte.setSpacing(false);
        spalte.getStyle()
                .set("flex", "1").set("background", "#f5f2ff").set("min-height", "100vh")
                .set("height", "100%").set("display", "flex").set("flex-direction", "column")
                .set("overflow", "hidden");
        spalte.getElement().setAttribute("tour-id", "warenkorb-spalte");
        spalte.add(new WarenkorbHeader(this::warenkorbLeeren), warenkorbPositionenLayout, zusammenfassung);
        return spalte;
    }

    // ═══════════════════════════════════════════════════════════
    // WARENKORB STATE
    // ═══════════════════════════════════════════════════════════

    /**
     * Legt einen Artikel in den Warenkorb oder erhöht dessen Menge.
     * Prüft ob der Bestand ausreicht und zeigt eine Fehlermeldung wenn nicht.
     *
     * @param artikel der hinzuzufügende Artikel
     */
    private void artikelZumKorbHinzufuegen(Artikel artikel) {
        if (artikel.getBestand() == 0) {
            FehlerUI.fehler("\"" + artikel.getName() + "\" ist nicht mehr auf Lager.");
            return;
        }

        for (WarenkorbEintrag e : warenkorbListe) {
            if (e.artikel.getId().equals(artikel.getId())) {
                if (artikel.getBestand() < 999 && e.menge >= artikel.getBestand()) {
                    FehlerUI.fehler("Nicht mehr Bestand vorhanden als bereits im Warenkorb.");
                    return;
                }
                e.menge++;
                aktualisiereWarenkorbUI();
                return;
            }
        }
        warenkorbListe.add(new WarenkorbEintrag(artikel, 1));
        aktualisiereWarenkorbUI();
    }

    /**
     * Zeichnet die Warenkorb-Positionen neu, aktualisiert die Preise
     * und aktualisiert die Bestandsbadges aller Artikelkarten.
     */
    private void aktualisiereWarenkorbUI() {
        warenkorbListe.removeIf(e -> e.menge <= 0);

        warenkorbPositionenLayout.removeAll();
        boolean zebra = false;
        for (WarenkorbEintrag e : new ArrayList<>(warenkorbListe)) {
            warenkorbPositionenLayout.add(
                    WarenkorbPositionFactory.create(e, zebra, () -> {
                        warenkorbListe.removeIf(x -> x.menge <= 0);
                        aktualisiereWarenkorbUI();
                    })
            );
            zebra = !zebra;
        }
        zusammenfassung.aktualisierePreise(warenkorbListe);
        aktualisiereBestandBadges();
    }

    /**
     * Berechnet für jeden Artikel den aktuell anzuzeigenden Bestand
     * (DB-Bestand minus was gerade im Warenkorb liegt) und aktualisiert
     * den Badge in der Karte per JS – ohne die Karte neu zu bauen.
     */
    private void aktualisiereBestandBadges() {
        for (Map.Entry<Long, Div> entry : kartenMap.entrySet()) {
            Long artikelId = entry.getKey();
            Div  karte     = entry.getValue();

            int imKorb = warenkorbListe.stream()
                    .filter(e -> e.artikel.getId().equals(artikelId))
                    .mapToInt(e -> e.menge)
                    .sum();

            int dbBestand = warenkorbListe.stream()
                    .filter(e -> e.artikel.getId().equals(artikelId))
                    .map(e -> e.artikel.getBestand())
                    .findFirst()
                    .orElse(0);

            if (imKorb > 0) {
                int anzeige = dbBestand >= 999 ? 999 : Math.max(0, dbBestand - imKorb);
                ArtikelKarteFactory.aktualisiereBestand(karte, artikelId, anzeige);
            } else {
                artikelService.findAllArtikel().stream()
                        .filter(a -> a.getId().equals(artikelId))
                        .findFirst()
                        .ifPresent(a -> ArtikelKarteFactory.aktualisiereBestand(
                                karte, artikelId, a.getBestand()));
            }
        }
    }

    /**
     * Leert den Warenkorb, setzt den Rabatt zurück und aktualisiert die UI.
     */
    private void warenkorbLeeren() {
        warenkorbListe.clear();
        zusammenfassung.resetRabatt();
        aktualisiereWarenkorbUI();
    }

    // ═══════════════════════════════════════════════════════════
    // GESCHÄFTSLOGIK
    // ═══════════════════════════════════════════════════════════

    /**
     * Schließt den Verkauf ab: berechnet Rabatt und Gesamtsumme, persistiert alle
     * Verkaufspositionen, aktualisiert den Bestand und öffnet den Quittungsdialog.
     *
     * @param zahlungsart die gewählte Zahlungsart (BAR oder KARTE)
     */
    private void verkaufAbschliessen(Zahlungsart zahlungsart) {
        try {
            BigDecimal rabatt = zusammenfassung.getAktuellerRabattProzent();

            List<Verkaufsposition> positionen = new ArrayList<>();
            for (WarenkorbEintrag e : warenkorbListe) {
                Verkaufsposition pos = new Verkaufsposition();
                pos.setArtikel(e.artikel);
                pos.setMenge(e.menge);
                pos.setEinzelpreis(e.artikel.getPreis());
                positionen.add(pos);
            }

            Verkauf verkauf = verkaufService.verkaufKomplett(positionen, zahlungsart, rabatt);
            letzterVerkaufId = verkauf.getId();

            FehlerUI.erfolg("Zahlung per " + zahlungsart.name() + " erfolgreich! Betrag: "
                    + WarenkorbZusammenfassung.format(verkauf.getGesamtsumme()));

            final List<Verkaufsposition> bonPositionen = new ArrayList<>(positionen);
            final Long verkaufIdFinal = verkauf.getId();

            QuittungsDialog quittungsDialog = new QuittungsDialog(
                    () -> { druckeKassenbon(bonPositionen, rabatt); warenkorbLeeren(); ladeArtikelGrid(); Broadcaster.broadcast("bestand-geaendert"); },
                    () -> { warenkorbLeeren(); ladeArtikelGrid(); Broadcaster.broadcast("bestand-geaendert"); },
                    () -> { verkaufStornieren(verkaufIdFinal); }
            );
            quittungsDialog.addOpenedChangeListener(e -> {
                if (!quittungsDialog.isOpened()) { warenkorbLeeren(); ladeArtikelGrid(); }
            });
            quittungsDialog.open();

        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
        }
    }

    /**
     * Storniert den zuletzt abgeschlossenen Verkauf, bucht den Bestand zurück
     * und gibt eine Erfolgsmeldung aus.
     *
     * @param verkaufId ID des zu stornierenden Verkaufs
     */
    private void verkaufStornieren(Long verkaufId) {
        try {
            verkaufService.verkaufStornieren(verkaufId);
            Broadcaster.broadcast("bestand-geaendert");
            warenkorbLeeren();
            ladeArtikelGrid();
            FehlerUI.erfolg("Verkauf wurde erfolgreich storniert.");
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
        } catch (Exception ex) {
            FehlerUI.technischerFehler(ex);
        }
    }

    /**
     * Generiert den Kassenbon als PDF und löst den Browser-Download aus.
     *
     * @param positionen die Verkaufspositionen des abgeschlossenen Verkaufs
     * @param rabatt     der angewendete Rabatt in Prozent
     */
    private void druckeKassenbon(List<Verkaufsposition> positionen, BigDecimal rabatt) {
        try {
            byte[] pdfBytes = pdfExportService.exportiereKassenbon(positionen, rabatt);
            String dateiname = "Kassenbon_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);
            UI.getCurrent().getPage().executeJs(
                    "const bytes=atob($0);const arr=new Uint8Array(bytes.length);" +
                            "for(let i=0;i<bytes.length;i++)arr[i]=bytes.charCodeAt(i);" +
                            "const blob=new Blob([arr],{type:'application/pdf'});" +
                            "const url=URL.createObjectURL(blob);" +
                            "const a=document.createElement('a');a.href=url;a.download=$1;" +
                            "document.body.appendChild(a);a.click();" +
                            "document.body.removeChild(a);URL.revokeObjectURL(url);",
                    base64, dateiname);
        } catch (KassensystemException ex) {
            FehlerUI.fehler(ex.getMessage());
        } catch (Exception ex) {
            FehlerUI.fehler("Fehler beim Bon-Druck. Bitte versuche es erneut.");
        }
    }

    /**
     * Erstellt einen Material-Symbols-Icon-Span.
     *
     * @param iconName Icon-Name (z.B. "search")
     */
    private Span createIcon(String iconName) {
        Span icon = new Span(iconName);
        icon.addClassName("material-symbols-outlined");
        icon.getStyle().set("line-height", "1");
        return icon;
    }

    /**
     * Prüft vor dem Rendern ob der Benutzer eingeloggt ist und leitet sonst zur Login-Seite weiter.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            event.rerouteTo("login");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TOUR-AKTIONEN
    // ═══════════════════════════════════════════════════════════

    /**
     * Verarbeitet Tour-Aktionen aus dem TourManager.
     * Wird als actionHandler an tourManager.start() übergeben.
     */
    public void tourAktion(String action) {
        switch (action) {
            case "demo-verkauf" -> {
                if (warenkorbListe.isEmpty()) {
                    artikelService.findAllArtikel().stream()
                            .filter(a -> a.isAktiv() && a.getBestand() > 0)
                            .findFirst()
                            .ifPresent(this::artikelZumKorbHinzufuegen);
                }
            }
            case "open-zahlungsdialog" -> {
                String betrag = zusammenfassung.getGesamtBetragText();
                String anzeige = (betrag == null || betrag.isBlank()) ? "2,99€" : betrag;
                new ZahlungsDialog(anzeige, art -> {}).open();
            }
            case "open-quittungsdialog" -> new QuittungsDialog(
                    () -> {},
                    () -> {},
                    () -> FehlerUI.erfolg("Stornierung war Erfolgreich.")
            ).open();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BROADCASTER
    // ═══════════════════════════════════════════════════════════

    /**
     * Registriert den Broadcaster-Listener wenn die View geöffnet wird.
     * Reagiert auf "bestand-geaendert" und "lager-geaendert" Events
     * und lädt das Artikel-Grid neu damit der Kassierer stets den aktuellen Bestand sieht.
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        broadcasterRegistration = Broadcaster.register(event -> {
            if ("bestand-geaendert".equals(event) || "lager-geaendert".equals(event)) {
                attachEvent.getUI().access(() -> {
                    try {
                        pruefeWarenkorb();
                        ladeArtikelGrid();
                    } catch (Exception ex) {
                        FehlerUI.technischerFehler(ex);
                    }
                });
            }
        });
    }

    /**
     * Entfernt den Broadcaster-Listener wenn die View geschlossen/verlassen wird.
     * Verhindert Memory Leaks durch verwaiste Listener.
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    /**
     * Hilfsmethode, die überprüft, ob sich der Bestand eines Artikels verändert hat. Der Warenkorb wird
     * dementsprechend aktualisiert.
     */
    private void pruefeWarenkorb() {
        warenkorbListe.removeIf(e -> {
            Artikel aktuell = artikelService.findArtikelById(e.artikel.getId());
            return aktuell.getBestand() < e.menge;
        });
        aktualisiereWarenkorbUI();
    }
}