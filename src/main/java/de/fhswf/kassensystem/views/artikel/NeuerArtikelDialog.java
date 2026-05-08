package de.fhswf.kassensystem.views.artikel;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.fhswf.kassensystem.model.Artikel;
import de.fhswf.kassensystem.service.ArtikelService;
import de.fhswf.kassensystem.views.components.FehlerUI;
import de.fhswf.kassensystem.views.components.BaseDialog;

/**
 * Dialog zum Anlegen und Bearbeiten von Artikeln.
 *
 * <p>Wird im Anlegen-Modus ohne Artikel-Parameter geöffnet.
 * Im Bearbeiten-Modus wird ein bestehender {@link de.fhswf.kassensystem.model.Artikel}
 * übergeben und die Felder werden vorausgefüllt.
 *
 * <p>Enthält {@link ArtikelFormularFelder} für die Eingabefelder
 * und {@link ArtikelBildUpload} für das optionale Artikelbild.
 *
 * @author Adrian Krawietz
 */
public class NeuerArtikelDialog extends BaseDialog {

    private final ArtikelService        artikelService;
    private final ArtikelFormularFelder felder;
    private final ArtikelBildUpload     bildUpload;
    private       Artikel               zuBearbeitenderArtikel = null;

    /**
     * Öffnet den Dialog im Anlegen-Modus (leere Felder).
     *
     * @param artikelService Service für das Speichern des neuen Artikels
     */
    public NeuerArtikelDialog(ArtikelService artikelService) {
        this.artikelService = artikelService;
        this.felder         = new ArtikelFormularFelder(artikelService);
        this.bildUpload     = new ArtikelBildUpload();
        init("Neuer Artikel", null);
    }

    /**
     * Öffnet den Dialog im Bearbeiten-Modus mit vorausgefüllten Feldern.
     *
     * @param artikelService         Service für das Aktualisieren des Artikels
     * @param artikel                der zu bearbeitende Artikel
     */
    public NeuerArtikelDialog(ArtikelService artikelService, Artikel artikel) {
        this.artikelService         = artikelService;
        this.felder                 = new ArtikelFormularFelder(artikelService);
        this.bildUpload             = new ArtikelBildUpload();
        this.zuBearbeitenderArtikel = artikel;
        felder.befuelleFelder(artikel);
        if (artikel.getBild() != null) bildUpload.setBild(artikel.getBild());
        init("Artikel bearbeiten", null);
    }

    /**
     * Baut den Dialog-Body mit Formularfeldern und Bild-Upload.
     */
    @Override
    protected VerticalLayout buildBody() {
        VerticalLayout body = new VerticalLayout();
        body.setWidthFull();
        body.setPadding(false);
        body.setSpacing(false);
        body.getStyle().set("padding", "1.5rem").set("gap", "1.25rem").set("background", "white")
                .set("overflow-y", "auto").set("max-height", "65vh");
        body.add(felder, bildUpload);
        return body;
    }

    /**
     * Validiert die Eingaben und speichert den Artikel (Anlegen oder Aktualisieren).
     *
     * @return {@code true} bei Erfolg (Dialog schließt), {@code false} bei Validierungsfehlern
     */
    @Override
    protected boolean onSpeichern() {
        if (!felder.valide()) return false;
        Artikel artikel = felder.toArtikel();
        if (bildUpload.getBildBytes() != null) artikel.setBild(bildUpload.getBildBytes());

        if (zuBearbeitenderArtikel != null) {
            artikel.setId(zuBearbeitenderArtikel.getId());
            return FehlerUI.versuch(() -> {
                artikelService.updateArtikel(artikel);
                FehlerUI.erfolg("Artikel \"" + artikel.getName() + "\" wurde aktualisiert.");
            });
        } else {
            return FehlerUI.versuch(() -> {
                artikelService.createArtikel(artikel);
                FehlerUI.erfolg("Artikel \"" + artikel.getName() + "\" wurde erstellt.");
            });
        }
    }

    @Override protected String getDialogBreite() { return "36rem"; }
}