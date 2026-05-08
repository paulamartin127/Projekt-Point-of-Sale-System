# Kassensystem – Point of Sale System

Webbasiertes Kassensystem für ein Café, entwickelt im Rahmen eines Hochschulprojekts an der Fachhochschule Südwestfalen.

---

## Technologie-Stack

| Schicht | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Sicherheit | Spring Security |
| Frontend | Vaadin 25  |
| Datenbank | PostgreSQL (Produktion), H2 (Tests) |
| PDF-Export | iText 7 |
| Build | Maven |
| Sonstiges | Lombok, Spring Data JPA |

---

## Voraussetzungen

- Java 21+
- Maven 3.8+
- PostgreSQL (lokal auf Port `5432`, Datenbankname `kassensystem`)
- IntelliJ IDEA (empfohlen)

---

## Projekt starten (IntelliJ)

### 1. Repository klonen

```bash
git clone https://github.com/Adrian-Kraw/Projekt-Point-of-Sale-System.git
```

Dann in IntelliJ: **File → Open** → Projektordner auswählen. Maven lädt die Abhängigkeiten automatisch.

### 2. Datenbank einrichten

PostgreSQL muss laufen und eine leere Datenbank namens `kassensystem` existieren:

```plaintext
CREATE DATABASE kassensystem;
```

### 3. Lokale Konfiguration anlegen

Die Datei `src/main/resources/application-dev.yml` wird **nicht** ins Repository eingecheckt (steht in `.gitignore`). Lege sie lokal an:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kassensystem
    username: postgres
    password: DEIN_PASSWORT
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

### 4. Dev-Profil in IntelliJ aktivieren

1. Oben rechts auf **Edit Configurations...** klicken
2. Die Spring Boot Run Configuration auswählen (z.B. `KassensystemApplication`)
3. Unter **Active Profiles** den Wert `dev` eintragen
4. Übernehmen und speichern

### 5. Anwendung starten

Die grüne **Run**-Schaltfläche in IntelliJ drücken oder `Shift + F10`.

Alternativ per Maven in PowerShell:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Die Anwendung ist erreichbar unter: [http://localhost:8080](http://localhost:8080)

Beim ersten Start legt der `DataInitializer` automatisch alle Stammdaten (Artikel, Kategorien, MwSt-Sätze) sowie die Standard-Benutzer an.

---

## Zugangsdaten

| Rolle | Benutzername | Passwort    |
|---|---|-------------|
| Manager | `Tobias` | `tobias123` |
| Kassierer | `Stefan` | `stefan123` |

---

## Features

### Verkauf
Artikel nach Kategorie filtern, Warenkorb befüllen, Zahlung per Bar oder Karte abwickeln, Quittung drucken.

### Lagerverwaltung
Bestandsübersicht mit Ampelstatus (grün/gelb/rot), Nachbestellhinweise bei Unterschreitung des Minimalbestands, Wareneingang buchen, ausstehende Lieferungen bestätigen oder stornieren.

### Artikelverwaltung
Artikel anlegen, bearbeiten und deaktivieren (Soft-Delete), Bild hochladen, Kategorien und MwSt-Sätze zuweisen. Nur für Manager zugänglich.

### Benutzerverwaltung
Benutzer anlegen, Rollen (Manager / Kassierer) vergeben, Passwörter ändern, Konten deaktivieren. Nur für Manager zugänglich.

### Berichte
Umsatzübersicht, Tagesabschluss, Top-Seller-Ranking, Zahlungsarten-Auswertung, PDF-Export. Nur für Manager zugänglich.

### Echtzeit-Updates
Broadcaster-Mechanismus sorgt dafür, dass Bestandsänderungen und neue Wareneingänge sofort bei allen aktiven Clients angezeigt werden.

---

## Rollen & Berechtigungen

| Funktion | Kassierer | Manager |
|---|:---:|:---:|
| Verkauf durchführen | ✅ | ✅ |
| Lagerübersicht einsehen | ✅ | ✅ |
| Lieferung bestätigen / stornieren | ✅ | ✅ |
| Wareneingang buchen | ❌ | ✅ |
| Artikel verwalten | ❌ | ✅ |
| Benutzer verwalten | ❌ | ✅ |
| Berichte & Statistiken | ❌ | ✅ |

---

## Projektstruktur

```
src/
├── main/
│   ├── java/de/fhswf/kassensystem/
│   │   ├── broadcast/              # Echtzeit-Broadcaster für Live-Updates zwischen Clients
│   │   ├── exception/              # Anwendungsspezifische Ausnahmen (ArtikelNotFoundException, ...)
│   │   ├── model/
│   │   │   ├── enums/              # Enums: Rolle, Status, WareneingangStatus, Zahlungsart
│   │   │   └── dto/                # DTOs für Berichte (ArtikelStatistikDTO, BelegDTO, ...)
│   │   ├── repository/             # Spring Data JPA Repositories
│   │   ├── security/               # Spring Security Konfiguration, UserDetailsService, SecurityUtils
│   │   ├── service/                # Geschäftslogik (ArtikelService, LagerService, VerkaufService, ...)
│   │   ├── tour/                   # Onboarding-Tour Komponenten
│   │   └── views/
│   │       ├── artikel/            # Artikelverwaltung (nur Manager)
│   │       ├── benutzer/           # Benutzerverwaltung (nur Manager)
│   │       ├── berichte/           # Berichte & Statistiken (nur Manager)
│   │       ├── components/         # Wiederverwendbare UI-Komponenten (BaseDialog, FehlerUI, ...)
│   │       ├── lager/              # Lagerverwaltung (Kassierer & Manager)
│   │       ├── sidebar/            # Navigation & Sidebar-Komponenten
│   │       └── verkauf/            # Kassenfunktion (Kassierer & Manager)
│   └── resources/
│       └── application.yaml        # Konfiguration (Profile: dev, prod, test)
│
└── test/
    └── java/de/fhswf/kassensystem/
        ├── broadcast/              # Tests für den Broadcaster
        ├── repository/             # Integrationstests für alle Repositories (H2)
        ├── security/               # Tests für SecurityUtils und UserDetailsService
        ├── service/                # Unit-Tests für alle Services
        ├── tour/                   # Tests für Tour-Logik
        └── views/                  # Tests für View-Hilfsklassen (BerichteUtils, ArtikelKarteFactory, ...)
```

---

## Tests ausführen

```powershell
mvn test
```

Tests laufen gegen eine H2-In-Memory-Datenbank (Profil `test`) – PostgreSQL wird dafür nicht benötigt.

---

## Entwickler

Entwickelt von **Paula Martin** und **Adrian Krawietz**