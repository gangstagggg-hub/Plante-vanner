# 🌱 Vanning – Push-server (backend)

Denne lille Node.js-serveren gjør at appen kan sende **ekte push-varsler** —
selv når appen er helt lukket på telefonen. Den gjør tre ting:

1. Tar imot og lagrer push-abonnement fra appen din (`/api/subscribe`)
2. Mottar plantelisten din hver gang du legger til, redigerer eller vanner en plante (`/api/plants`)
3. Sjekker hver dag (kl. 08:00 som standard) om noen planter trenger vann, og sender push-varsel

Data lagres i en enkel fil (`data.json`) — ingen ekstern database er nødvendig.

## 1. Kjør lokalt (valgfritt, for testing)

```bash
cd server
npm install
npm start
```

Serveren starter på `http://localhost:3000`. En `.env`-fil med ferdig genererte
VAPID-nøkler følger allerede med, så du trenger ikke gjøre noe mer for å teste lokalt.

## 2. Publiser serveren (nødvendig for at push skal virke fra telefonen)

Backend må ligge på en adresse som er tilgjengelig fra internett (i motsetning til
GitHub Pages, som kun kan hoste den statiske app-delen). Enkleste gratis-alternativ er **Render.com**:

1. Opprett en konto på [render.com](https://render.com) og koble den til GitHub-kontoen din.
2. Push denne `server`-mappen til et eget GitHub-repo (f.eks. `vanning-push-server`).
   - **Viktig:** `.env`-filen er lagt til i `.gitignore` og lastes derfor ikke opp automatisk — det er riktig, siden den inneholder hemmelige nøkler.
3. På Render: **New → Web Service** → velg repoet ditt.
4. Sett:
   - **Build Command:** `npm install`
   - **Start Command:** `npm start`
5. Under **Environment**, legg inn miljøvariablene fra `.env`-filen din manuelt (kopier verdiene):
   - `VAPID_PUBLIC_KEY`
   - `VAPID_PRIVATE_KEY`
   - `VAPID_SUBJECT`
   - `NOTIFY_HOUR` (f.eks. `8`)
   - `ALLOWED_ORIGIN` — sett denne til adressen til app-en din, f.eks. `https://dittbrukernavn.github.io`
6. Trykk **Create Web Service**. Etter noen minutter får du en adresse som
   `https://vanning-push-server.onrender.com`.

> Gratisnivået på Render "sovner" etter inaktivitet, men våkner automatisk igjen. Cron-jobben kjører kun mens serveren er våken. Vil du ha garantert daglig varsling uansett, må du enten bruke en betalt "always on"-plan, eller sette opp en ekstern "ping"-tjeneste (f.eks. cron-job.org) som besøker `/api/health` en gang i timen for å holde serveren våken.

## 3. Koble frontend til backend

Åpne `index.html` i app-mappen og finn linjen:

```js
const BACKEND_URL = ''; // TODO: sett inn URL til backend her
```

Endre til adressen du fikk fra Render, uten skråstrek på slutten:

```js
const BACKEND_URL = 'https://vanning-push-server.onrender.com';
```

Last opp den oppdaterte `index.html` til GitHub Pages-repoet ditt på nytt. Neste
gang du slår på 🔔-knappen i appen, registreres et ekte push-abonnement mot
serveren din, og du får varsler selv om appen er lukket.

## Vil du bruke dine egne VAPID-nøkler?

```bash
node generate-vapid-keys.js
```

Legg de nye nøklene i `.env`, **og** oppdater `VAPID_PUBLIC_KEY`-konstanten i
`index.html` slik at den matcher den nye offentlige nøkkelen.

## Endepunkter

| Metode | Endepunkt | Beskrivelse |
|---|---|---|
| GET | `/api/health` | Sjekk at serveren lever |
| GET | `/api/vapid-public-key` | Hent offentlig VAPID-nøkkel |
| POST | `/api/subscribe` | Registrer push-abonnement for en enhet |
| POST | `/api/unsubscribe` | Fjern push-abonnement |
| POST | `/api/plants` | Synkroniser planteliste for en enhet |
| POST | `/api/check-now` | Trigger en manuell vanningssjekk (nyttig for testing) |

## Sikkerhet

- `.env` og `data.json` er lagt til i `.gitignore` — **ikke** fjern dem derfra, og ikke last disse filene opp til et offentlig repo manuelt.
- `VAPID_PRIVATE_KEY` skal aldri deles eller ligge i frontend-koden.
- Sett `ALLOWED_ORIGIN` til den faktiske app-adressen din i produksjon, i stedet for `*`, slik at kun din egen app kan sende data til serveren.
