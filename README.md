# 🌱 Vanning — Plante App

En elegant PWA (Progressive Web App) som minner deg på når plantene dine trenger vann.

## Funksjoner

- Legg til planter med navn, rom, vanningsintervall og mengde vann (obligatorisk), pluss valgfritt bilde, familie og maks høyde
- Hele plantekortet skifter farge etter hvor nær vanningsdagen er (grønn → gul → rød)
- Hold inne i 2 sekunder på et kort for å registrere at du har vannet
- Dobbeltklikk/dobbelttrykk på et kort for å redigere eller slette planten
- Varsler (🔔-knappen øverst) sender en notifikasjon når en plante trenger vann
- All data lagres i nettleseren (`localStorage`) — status er riktig hver gang du åpner appen, selv dager senere
- Installerbar på hjemskjermen på mobil (PWA), med egen app-ikon og offline-støtte

## Om varslene

Trykk på 🔔-knappen øverst i appen for å slå på varsler. Appen sjekker automatisk
lokalt hver gang du åpner den om noen planter trenger vann i dag.

**For at varsler skal fungere selv når appen er helt lukket**, følger det med en
egen backend push-server i `server`-mappen. Se `server/README.md` for hvordan du
publiserer den (tar ca. 10 minutter, gratis på Render.com) og kobler den til appen.
Uten denne serveren fungerer varslene kun mens du har appen åpen eller nylig åpnet.

## Slik legger du den ut på GitHub Pages

1. Opprett et nytt repository på GitHub, f.eks. `vanning-app`.
2. Last opp **alle filene i denne mappen** til repoet (behold mappestrukturen, spesielt `icons/`-mappen).
3. Gå til repoets **Settings → Pages**.
4. Under **Source**, velg branchen `main` og mappen `/ (root)`, og lagre.
5. Etter et minutt eller to er appen tilgjengelig på `https://dittbrukernavn.github.io/vanning-app/`.
6. Åpne adressen på mobilen — første gang vil du få tilbud om å legge appen til på hjemskjermen.

> PWA-installasjon krever HTTPS, som GitHub Pages gir deg automatisk.

## Filoversikt

```
index.html          – selve appen (HTML/CSS/JS)
manifest.json        – PWA-metadata (navn, farger, ikoner)
sw.js                 – service worker for offline-støtte og push-varsler
icons/                – app-ikon i alle nødvendige størrelser
icon-master.svg        – kildefil for ikonet (kan redigeres videre)
icon-maskable.svg      – kildefil for Android sin maskable ikon-variant
server/                – backend push-server (se server/README.md)
```

## Om datalagringen

Plantene lagres lokalt i nettleseren på hver enhet (`localStorage`). Det betyr at data **ikke synkroniseres** mellom telefon og PC, og forsvinner hvis du tømmer nettleserdata. Ønsker du synkronisering på tvers av enheter senere, trengs det en backend/database (f.eks. Firebase eller Supabase) — si ifra, så kan vi bygge det inn.

## Videre app-konvertering

Denne PWA-en kan installeres direkte fra nettleseren og fungerer som en app på både iOS og Android uten App Store. Ønsker du en ekte native app (Swift/Kotlin) eller vil pakke den med f.eks. Capacitor, er HTML/CSS/JS-koden i `index.html` et solid utgangspunkt.
