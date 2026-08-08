require('dotenv').config();
const express = require('express');
const cors = require('cors');
const webpush = require('web-push');
const cron = require('node-cron');
const fs = require('fs');
const path = require('path');

const DB_FILE = path.join(__dirname, 'data.json');
const PORT = process.env.PORT || 3000;
const NOTIFY_HOUR = process.env.NOTIFY_HOUR || '8'; // time of day (server time) for the daily check, 24h format
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || '*'; // set to your GitHub Pages URL in production

if (!process.env.VAPID_PUBLIC_KEY || !process.env.VAPID_PRIVATE_KEY) {
  console.error('Mangler VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY i miljøvariabler. Se .env.example.');
  process.exit(1);
}

webpush.setVapidDetails(
  process.env.VAPID_SUBJECT || 'mailto:example@example.com',
  process.env.VAPID_PUBLIC_KEY,
  process.env.VAPID_PRIVATE_KEY
);

// --- Enkel fil-basert database ---
// Struktur: { "<deviceId>": { subscription: {...}, plants: [...] } }
function readDB() {
  try {
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch (e) {
    return {};
  }
}
function writeDB(db) {
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
}

const app = express();
app.use(cors({ origin: ALLOWED_ORIGIN }));
app.use(express.json({ limit: '2mb' }));

// Offentlig VAPID-nøkkel (kan hentes av klienten i stedet for å hardkodes)
app.get('/api/vapid-public-key', (req, res) => {
  res.json({ publicKey: process.env.VAPID_PUBLIC_KEY });
});

// Registrer/oppdater push-abonnement for en enhet
app.post('/api/subscribe', (req, res) => {
  const { deviceId, subscription } = req.body;
  if (!deviceId || !subscription) {
    return res.status(400).json({ error: 'deviceId og subscription er påkrevd' });
  }
  const db = readDB();
  db[deviceId] = db[deviceId] || {};
  db[deviceId].subscription = subscription;
  db[deviceId].plants = db[deviceId].plants || [];
  writeDB(db);
  res.json({ ok: true });
});

// Fjern abonnement (varsler skrudd av)
app.post('/api/unsubscribe', (req, res) => {
  const { deviceId } = req.body;
  const db = readDB();
  if (db[deviceId]) {
    delete db[deviceId].subscription;
    writeDB(db);
  }
  res.json({ ok: true });
});

// Synkroniser planteliste for en enhet (kalles hver gang plantedata endres i appen)
app.post('/api/plants', (req, res) => {
  const { deviceId, plants } = req.body;
  if (!deviceId || !Array.isArray(plants)) {
    return res.status(400).json({ error: 'deviceId og plants (array) er påkrevd' });
  }
  const db = readDB();
  db[deviceId] = db[deviceId] || {};
  db[deviceId].plants = plants;
  writeDB(db);
  res.json({ ok: true });
});

app.get('/api/health', (req, res) => res.json({ ok: true, time: new Date().toISOString() }));

// --- Kjernelogikk: regn ut om en plante trenger vann i dag ---
function daysLeftFor(plant) {
  const msPerDay = 86400000;
  const last = new Date(plant.lastWatered);
  last.setHours(0, 0, 0, 0);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const diffDays = Math.round((now - last) / msPerDay);
  return plant.intervall - diffDays;
}

async function sendPush(subscription, payload) {
  try {
    await webpush.sendNotification(subscription, JSON.stringify(payload));
    return true;
  } catch (err) {
    if (err.statusCode === 410 || err.statusCode === 404) {
      return 'expired'; // abonnementet finnes ikke lenger (appen avinstallert e.l.)
    }
    console.error('Push-feil:', err.statusCode, err.message);
    return false;
  }
}

async function checkAllAndNotify() {
  const db = readDB();
  const today = new Date().toISOString().slice(0, 10);
  let changed = false;

  for (const deviceId of Object.keys(db)) {
    const entry = db[deviceId];
    if (!entry.subscription || !Array.isArray(entry.plants)) continue;

    for (const plant of entry.plants) {
      const daysLeft = daysLeftFor(plant);
      if (daysLeft <= 0 && plant.lastNotified !== today) {
        const title = `🌱 ${plant.navn} trenger vann`;
        const body = `${plant.rom} · ${plant.mengde}${daysLeft < 0 ? ` — forsinket ${Math.abs(daysLeft)} dager` : ' — i dag'}.`;
        const result = await sendPush(entry.subscription, { title, body });
        if (result === 'expired') {
          delete entry.subscription;
          changed = true;
          break;
        }
        if (result === true) {
          plant.lastNotified = today;
          changed = true;
        }
      }
    }
  }

  if (changed) writeDB(db);
}

// Daglig sjekk kl. NOTIFY_HOUR (server-tid)
cron.schedule(`0 ${NOTIFY_HOUR} * * *`, () => {
  console.log('Kjører daglig vanningssjekk...');
  checkAllAndNotify();
});

// Ekstra manuell trigger for testing: POST /api/check-now
app.post('/api/check-now', async (req, res) => {
  await checkAllAndNotify();
  res.json({ ok: true });
});

app.listen(PORT, () => {
  console.log(`Vanning-server kjører på port ${PORT}`);
  console.log(`Daglig varslingssjekk kjører kl. ${NOTIFY_HOUR}:00 (server-tid)`);
});
