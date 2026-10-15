# Pubblicazione su Google Play — UV Profiles

## Cosa c'è in questa cartella

| File | Uso |
|---|---|
| `icon-512.png` | Icona 512x512 per la scheda dello store |
| `feature-graphic-1024x500.png` | Banner in cima alla scheda |

Entrambe sono generate da `tools/make_store_icon.ps1` e
`tools/make_feature_graphic.ps1`. Se cambi il disegno, rigenerale da lì
invece di editare i PNG a mano.

## Build da caricare

```
gradlew bundleRelease
```

Produce `app/build/outputs/bundle/release/app-release.aab` (~650 KB).
Quello che si carica nella console è **solo l'AAB**, non l'APK.

## Firma

La chiave di upload è in `keystore/uvprofiles-upload.jks` (alias
`uvprofiles`, RSA 4096, validità 27 anni). I dati di accesso sono in
`keystore.properties`, che è in `.gitignore`.

**Se perdi quella chiave non puoi più aggiornare l'app su Play.**
Conserva `keystore/` e `keystore.properties` in un posto sicuro, e non
solo su questa macchina.

Nella console, in *Play App Signing*, puoi far generare a Google anche
una chiave dell'app: è una misura in più, non un sostituto.

## targetSdk

`targetSdk 36` (Android 16). Dal 31 agosto 2026 Google Play non accetta
né nuove app né aggiornamenti con un targetApi inferiore, quindi è il
minimo per essere pubblicati.

In questo progetto c'era prima `targetSdk 34`, con un commento che
diceva che su Android 15 le app con targetSdk 35 non riescono a eseguire
`su`. **Era una conclusione sbagliata**, e per due motivi:

1. Il grant di Magisk è legato all'uid. Disinstallando l'app e
   reinstallandola l'uid cambia, e senza autorizzazione esplicita Magisk
   non concede più root. Il sintomo (nessun dato letto) è identico a
   quello di un `su` bloccato, da qui la diagnosi errata.
2. `/data/adb/ksu` esisteva sul dispositivo, residuo di un'installazione
   precedente, e faceva pensare a KernelSU. In realtà è attivo **Magisk
   30.7** (`su -v` → `30.7:MAGISKSU`, `magiskd` in esecuzione).

Verificato sul device con lo stesso uid: `targetSdk` 34, 35 e 36 danno
tutti root funzionante, pulsanti profilo compresi.

Il valore resta sovrascrivibile:

```
gradlew assembleDebug -PtargetSdkOverride=34
```

## Cosa serve ancora per pubblicare

1. **Account sviluppatore Play** (25 USD una tantum, il titolare deve
   avere almeno 18 anni e superare la verifica).
2. **Policy sulla privacy** con URL pubblico. È obbligatoria anche se
   l'app non raccoglie nulla.
3. **Dichiarazioni "Contenuto dell'app"**: privacy policy, annunci,
   classificazione etica, target utenti, funzionalità degli strumenti
   di sicurezza.
4. **Scheda del negozio**: descrizione breve e completa, categoria,
   almeno 2 screenshot per telefono.
5. **Test chiuso** se l'account è stato creato dopo il 13 novembre
   2023: 12 tester per 14 giorni consecutivi prima di chiedere
   l'accesso alla produzione.

## Punto delicato: l'app usa root

L'app invoca `su` per leggere e scrivere in `/sys`. Non richiede moduli
Magisk: i profili sono nello script `res/raw/uv_profiles.sh` dentro l'app.
Serve però un gestore root installato e autorizzato (qui Magisk 30.7), al
trimenti l'app non fa nulla.

- Va detto chiaramente nella descrizione, altrimenti le recensioni
  saranno negative: chi non ha root installa l'app e vede solo "?" e
  un avviso.
- Google può chiedere di spiegare come l'app usa root e cosa tocca.
  Meglio anticiparlo.
- Su questo kernel l'app **non tocca le tensioni**: le scrive in
  `/sys/power/percent_margin` e i cap di frequenza. Vale la pena
  dirlo, perché "undervolt" fa pensare alle tensioni e qui non è vero.
- L'app non ha il permesso `INTERNET` e non raccoglie dati: nel
  questionario *Sicurezza dei dati* si dichiara "nessun dato
  raccolto", il che semplifica molto la scheda.

## Test su altri dispositivi

I profili sono tarati su questo kernel. Su un device diverso i nomi dei
file in `/sys/power/percent_margin` possono non esistere e i cap
verrebbero rifiutati: l'app non lo segnala, perché non c'è un modo pulito
di sapere in anticipo quale kernel si ha sotto mano.

## Build non firmate in minificazione

`minifyEnabled` è `false`. Per un'app di queste dimensioni non cambia
la dimensione in modo apprezzabile; se in futuro si abbassa `minSdk` o
si aggiunge codice, valutare R8.