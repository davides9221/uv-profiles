# Scheda Google Play — testi

Vincoli: nome max 30 caratteri, descrizione breve max 80, descrizione
completa max 4000.

---

## Nome

```
UV Profiles
```

## Descrizione breve

```
Profili CPU/GPU per Exynos 990 con root. Nessun modulo richiesto.
```

(62 caratteri)

---

## Descrizione completa

```
UV Profiles gestisce i limiti di frequenza e i margini di tensione del
processore di un Samsung Galaxy Note 20 Ultra con chip Exynos 990,
tramite cinque profili pronti da applicare con un tocco.

L'app non richiede moduli Magisk: i profili sono inclusi nell'app. Serve
però un gestore root installato.

REQUISITI

- Smartphone con processore Exynos 990 (Note 20 Ultra 5G)
- Android 8.0 o successivo
- Root attivo con Magisk, KernelSU o APatch
- Autorizzazione root concessa all'app

Senza root l'app si apre, ma non fa nulla e lo segnala a schermo.

Cosa fa

Cinque profili, ognuno con due regolazioni:

- Margini di tensione per dominio: CPU (tre cluster), GPU, bus di
  memoria, dominio display e acceleratore neurale.
- Limiti di frequenza massima per ciascun cluster CPU.

ECO
  Frequenze ridotte e margini negativi: il profilo per risparmiare
  batteria. Adatto a navigazione, musica, uso diurno.

BALANCED
  Riduzione lieve. Il compromesso per l'uso quotidiano.

SIXCORE
  Il profilo consigliato. Tiene il cluster piu veloce a 1157 MHz,
  dove la scala delle frequenze e piu stretta, cosi non sale alle
  frequenze piu alte durante l'uso normale. Il cluster lento e quello
  medio restano liberi. Nessun core viene spento.

SPORT
  Nessun limite di frequenza e margini minimi. Prestazioni massime,
  con il consumo piu alto.

OFF
  Ripristina i valori originali del dispositivo. Non azzera: su questo
  processore lo zero non e il valore predefinito, quindi azzerare
  lascerebbe il telefono in una configurazione che non esiste.

Pannello stato

L'app mostra in tempo reale:

- frequenza corrente e massima dei tre cluster, con un grafico a barre
- margini di tensione di tutti i domini
- carico del sistema, temperatura della batteria, memoria disponibile
  e memoria di swap

Widget

Un widget per la schermata home con i quattro profili principali e il
ripristino, per cambiare profilo senza aprire l'app.

Cosa NON fa

- Non modifica le tensioni elettriche in senso assoluto. Su questo
  processore le tensioni GPU non sono scrivibili dal software, quindi
  l'app lavora solo sui margini percentuali e sui limiti di frequenza.
- Non richiede e non installa moduli.
- Non raccoglie nulla e non accede a internet: l'app non chiede
  il permesso di rete.

I valori riportati sono specifici di questo processore. Su un modello
diverso l'app non ha modo di accorgersi in anticipo e i pulsanti
potrebbero non avere effetto.
```

(circa 2500 caratteri, entro il limite di 4000)

---

## Note per chi riempie il modulo

**Categoria:** strumenti.

**Contenuto dell'app.** Le risposte per un'app che non raccoglie nulla:

- Raccolta dati: no
- Dati condivisi: no
- Raccolta dati raccolti in modo facoltativo: no
- Raccolta dati raccolti in modo obbligatorio: no
- I dati sono cifrati in transito: non applicabile
- È possibile richiedere la cancellazione dei dati: non applicabile

**Privacy policy.** Obbligatoria anche se non si raccoglie dati: va
pubblicata una pagina che lo dichiari e il suo URL inserito in
*Policy sulla privacy*.

**Altre dichiarazioni.** Dichiarare esplicitamente che l'app usa
l'accesso root per modificare parametri di sistema. Meglio anticiparlo
che aspettare la richiesta di Google.

**Classificazione etica.** 3+ (controllo remoto di un dispositivo).

**Target utenti.** 13+ o 18+; 18+ e la scelta piu prudente, perche
l'app tocca parametri di sistema.