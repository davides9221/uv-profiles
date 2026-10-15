# UV Profiles

App Android per gestire i profili CPU/GPU di un Samsung Galaxy Note 20
Ultra (Exynos 990) con root. Sostituisce i vecchi moduli Magisk: i
profili sono dentro l'app e si applicano con un tocco.

L'app non richiede moduli. Richiede pero un gestore root installato e
autorizzato (su questo dispositivo Magisk 30.7).

## Cosa fa

Cinque profili, ognuno formato da due cose:

- i **margini di tensione** per dominio, in `/sys/power/percent_margin`
  (percentuale rispetto alla tensione nominale, piu basso = meno
  tensione e piu efficienza)
- i **cap di frequenza** per cluster CPU, in
  `/sys/devices/system/cpu/cpufreq/policy*/scaling_max_freq`

| Profilo | LITTLE | MIDDLE | PRIME | Margini lit/mid/big |
|---|---|---|---|---|
| ECO | 1800 | 1794 | 2002 | -6 / -6 / -7 |
| BALANCED | 2002 | 2002 | 2002 | -4 / -4 / -5 |
| SIXCORE | 2002 | 1794 | 1157 | -3 / -4 / 0 |
| SPORT | max | max | max | -2 / -2 / -3 |
| OFF | fabbrica | fabbrica | fabbrica | fabbrica |

Frequenze in MHz.

`SIXCORE` e il profilo di uso quotidiano: tiene il cluster veloce a
1.157 GHz, dove la scala OPP e piu stretta, cosi non scala in
continuazione. Il nome viene da una versione iniziale che spegneva dei
core; ora **nessun core viene spento**, tutti restano online.

`OFF` riporta i valori di fabbrica, non azzera. La differenza conta:
su questo kernel lo `0` non e il default (`lit` e `-3`), quindi
azzerare lascerebbe il dispositivo in uno stato che non esiste.

## Struttura

```
app/src/main/
  kotlin/it/davide/uvprofiles/
    MainActivity.kt        pulsanti profilo, grafico e testo di stato
    UvReader.kt           parsing dell'output dello script di lettura
    RootShell.kt          esecuzione di su, scrittura script su file
    ClusterChartView.kt   grafico a barre dei tre cluster
    UvWidgetProvider.kt   widget home screen
  res/raw/
    uv_profiles.sh        profili e logica di applicazione
    uv_profiles_read.sh   lettura dei valori per il pannello STATUS
```

## Due dettagli non ovvi sugli script

**Gli script vivono in `res/raw`, non come stringhe Kotlin.** In Kotlin `$`
e il placeholder di interpolazione: uno script di shell ne e pieno, e
ogni `$` verrebbe risolto da Kotlin invece che dalla shell. Con variabili
diventate vuote, i percorsi di `/sys/power/percent_margin` (permessi
`600 root`) diventavano illeggibili e tornavano vuoti **in silenzio**,
perche l'errore era rediretto su `/dev/null`. Tenendo lo script in
`res/raw` i byte arrivano al file esattamente come scritti.

**Il separatore dei campi e `@`.** La shell interpreta `|` come pipe
anche dentro `$()`, e `:` rendeva fragile lo split sui valori negativi
dei margini.

## Persistenza al boot

L'app da sola non puo riapplicare il profilo dopo un riavvio: serve
qualcosa che giri all'avvio del sistema, e un'app non puo farlo. La
soluzione senza moduli e `device/service.d-uv-profiles.sh`.

Magisk esegue tutti gli script in `/data/adb/service.d/` dopo l'avvio.
Non e un modulo: e una cartella, quindi niente da installare o
disinstallare dalla sua interfaccia.

```
adb push device/service.d-uv-profiles.sh /data/local/tmp/
adb shell su -c 'cp /data/local/tmp/service.d-uv-profiles.sh /data/adb/service.d/uv-profiles.sh'
adb shell su -c 'chmod 755 /data/adb/service.d/uv-profiles.sh'
```

Lo script richiama `/data/adb/uv-profiles/apply.sh`, che e lo stesso
script di profili contenuto nell'app: i profili restano definiti in un
solo posto, e l'app lo riscrive a ogni avvio. Se quel file non esiste
lo script esce senza fare nulla.

Alternativa senza toccare nulla: **Termux + Termux:Boot**, che fa la
stessa cosa con un'app in piu.

## Conflitto con schedutil-uv2

Attenzione a questo modulo, se lo usi insieme.

Anche `schedutil-uv2` scrive i margini e i cap a ogni avvio, e i due
script girano in parallelo: **vince l'ultimo che scrive**. Con il solo
`service.d` (primo passaggio a 40 s dal boot) succedeva che
`schedutil-uv2` (45 s) sovrascrivesse il profilo scelto:

```
12:23:31  profilo sixcore applicato
12:23:36  schedutil-uv2 imposta policy4 2002000, policy6 2002000
          e i margini lit -3 / mid -3 / big -4 / g3d -2
```

Per questo lo script riapplica il profilo tre volte (a 40, 95 e 150
secondi dal boot): gli ultimi passaggi arrivano dopo e vincono. Serve
anche per il caso in cui `pm_qos` del vendor rifiuti una scrittura su
`scaling_max_freq` nelle prime decine di secondi dopo l'avvio.

Se vuoi eliminare il conflitto alla radice, `schedutil-uv2` andrebbe
ridotto alla sola parte governor: da solo sul questo kernel non fa
niente, perche il governor e gia `schedutil` e la cartella `schedutil/`
non esiste.

## Compilare

```
JAVA_HOME=... ANDROID_HOME=... gradlew assembleDebug
JAVA_HOME=... ANDROID_HOME=... gradlew bundleRelease
```

L'ambiente di sviluppo usato:

- JDK 17 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`)
- `ANDROID_HOME` = `%LOCALAPPDATA%\Android\Sdk`
- Gradle 9.6.0, AGP 9.0.0, Kotlin integrato in AGP (non applicare
  `org.jetbrains.kotlin.android`)
- `minSdk 26`, `targetSdk 36`, `compileSdk 36`

## Firma

Vedi `store/README.md` e `keystore/`. La chiave di upload non va nel
controllo versione e, se persa, non permette piu aggiornamenti su Play.