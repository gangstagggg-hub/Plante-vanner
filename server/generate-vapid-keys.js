// Kjør: node generate-vapid-keys.js
// Genererer et nytt VAPID-nøkkelpar for web push, i tilfelle du vil bruke egne
// nøkler i stedet for de som allerede følger med i .env.
const webpush = require('web-push');
const keys = webpush.generateVAPIDKeys();

console.log('Nytt VAPID-nøkkelpar generert:\n');
console.log('VAPID_PUBLIC_KEY=' + keys.publicKey);
console.log('VAPID_PRIVATE_KEY=' + keys.privateKey);
console.log('\nLegg disse inn i .env-filen din. Husk å bytte ut den offentlige');
console.log('nøkkelen (VAPID_PUBLIC_KEY) i icons/../index.html også, slik at');
console.log('frontend og backend bruker samme nøkkelpar.');
