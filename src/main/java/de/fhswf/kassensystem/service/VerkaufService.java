package de.fhswf.kassensystem.service;

import de.fhswf.kassensystem.exception.UngueltigeEingabeException;
import de.fhswf.kassensystem.model.Artikel;
import de.fhswf.kassensystem.model.Verkauf;
import de.fhswf.kassensystem.model.Verkaufsposition;
import de.fhswf.kassensystem.model.enums.Status;
import de.fhswf.kassensystem.model.enums.Zahlungsart;
import de.fhswf.kassensystem.repository.ArtikelRepository;
import de.fhswf.kassensystem.repository.VerkaufRepository;
import de.fhswf.kassensystem.repository.UserRepository;
import de.fhswf.kassensystem.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service für die Durchführung und Persistierung von Verkaufsvorgängen.
 *
 * <p>
 *     Kapselt die Geschäftslogik für den Kassiervorgang.
 * </p>
 *
 * @author Paula Martin, Adrian Krawietz
 */
@Service
public class VerkaufService {

    private VerkaufRepository verkaufRepository;
    private ArtikelRepository artikelRepository;
    private SecurityUtils securityUtils;
    private UserRepository userRepository;

    public VerkaufService(VerkaufRepository verkaufRepository,
                          UserRepository userRepository,
                          SecurityUtils securityUtils,
                          ArtikelRepository artikelRepository) {
        this.verkaufRepository = verkaufRepository;
        this.artikelRepository = artikelRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Schließt einen Verkaufsvorgang ab und persistiert ihn atomar.
     *
     * <p>Alle Schritte (Verkauf anlegen, Kassierer zuweisen, Positionen verknüpfen
     * und speichern) erfolgen in einer einzigen Datenbanktransaktion ({@code @Transactional}),
     * um inkonsistente Zustände und Race Conditions bei parallelen Kassenvorgängen
     * zu vermeiden.</p>
     *
     * @param positionen Liste der Verkaufspositionen mit Artikel, Menge und Einzelpreis
     * @param zahlungsart gewählte Zahlungsart (BAR oder KARTE)
     * @param rabatt gewährter prozentualer Rabatt, {@code 0} für keinen Rabatt
     * @return der persistierte {@link Verkauf} inklusive generierter ID
     */
    @Transactional
    public Verkauf verkaufKomplett(
            List<Verkaufsposition> positionen,
            Zahlungsart zahlungsart,
            BigDecimal rabatt) {

        if (positionen == null) {
            throw new IllegalArgumentException("Positionen dürfen nicht null sein.");
        }
        if (positionen.isEmpty()) {
            throw new UngueltigeEingabeException("Der Warenkorb darf nicht leer sein.");
        }
        if (zahlungsart == null) {
            throw new IllegalArgumentException("Zahlungsart darf nicht null sein.");
        }
        if (rabatt == null) {
            throw new IllegalArgumentException("Rabatt darf nicht null sein.");
        }
        if (rabatt.signum() < 0) {
            throw new IllegalStateException("Rabatt darf nicht kleiner als 0 sein.");
        }

        BigDecimal zwischensumme = positionen.stream()
                .map(p -> p.getEinzelpreis().multiply(BigDecimal.valueOf(p.getMenge())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rabattBetrag = rabatt.compareTo(BigDecimal.ZERO) > 0
                ? zwischensumme.multiply(rabatt)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal gesamtsumme = zwischensumme.subtract(rabattBetrag);

        // Bestandsabbuchung gehört in die Transaktion rein
        for (Verkaufsposition pos : positionen) {
            Artikel artikel = artikelRepository.findById(pos.getArtikel().getId())
                    .orElseThrow(() -> new UngueltigeEingabeException("Artikel nicht gefunden"));
            if (artikel.getBestand() < 999) {
                int neuerBestand = artikel.getBestand() - pos.getMenge();
                if (neuerBestand < 0) {
                    throw new UngueltigeEingabeException(
                            "Nicht genug Bestand für: " + artikel.getName());
                }
                artikel.setBestand(neuerBestand);
                artikelRepository.save(artikel);
            }
        }

        Verkauf verkauf = new Verkauf();
        verkauf.setTimestamp(LocalDateTime.now());
        verkauf.setStatus(Status.ABGESCHLOSSEN);
        verkauf.setZahlungsart(zahlungsart);
        verkauf.setRabatt(rabatt);
        verkauf.setGesamtsumme(gesamtsumme);

        securityUtils.getEingeloggterUser()
                .ifPresent(verkauf::setKassierer);

        // Positionen verknüpfen
        for (Verkaufsposition pos : positionen) {
            pos.setVerkauf(verkauf);
        }
        verkauf.setPositionen(positionen);

        return verkaufRepository.save(verkauf);
    }

    /**
     * Storniert einen abgeschlossenen Verkauf.
     *
     * <p>Setzt den Status auf {@link Status#STORNIERT} und bucht den Bestand
     * aller Positionen zurück. Die Operation ist atomar – schlägt ein
     * Bestandsupdate fehl, wird die gesamte Transaktion zurückgerollt.</p>
     *
     * @param verkaufId ID des zu stornierenden Verkaufs
     * @throws UngueltigeEingabeException wenn der Verkauf nicht gefunden wird oder
     *                                    bereits storniert ist
     */
    @Transactional
    public void verkaufStornieren(Long verkaufId) {
        Verkauf verkauf = verkaufRepository.findById(verkaufId)
                .orElseThrow(() -> new UngueltigeEingabeException("Verkauf nicht gefunden."));

        if (verkauf.getStatus() == Status.STORNIERT) {
            throw new UngueltigeEingabeException("Verkauf ist bereits storniert.");
        }

        for (Verkaufsposition pos : verkauf.getPositionen()) {
            Artikel artikel = pos.getArtikel();
            if (artikel.getBestand() < 999) {
                artikel.setBestand(artikel.getBestand() + pos.getMenge());
                artikelRepository.save(artikel);
            }
        }

        verkauf.setStatus(Status.STORNIERT);
        verkaufRepository.save(verkauf);
    }
}