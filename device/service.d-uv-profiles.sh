#!/system/bin/sh
# Applica il profilo salvato ad ogni avvio.
#
# Sostituisce il modulo uv-profiles: Magisk esegue gli script di questa
# cartella dopo l'avvio del sistema, quindi non serve alcun modulo.
#
# Lo script applicativo lo scrive l'app in /data/adb/uv-profiles/apply.sh,
# cosi i profili restano definiti in un posto solo.

BASE=/data/adb/uv-profiles
APPLY="$BASE/apply.sh"

# Se l'app non e mai stata aperta non c'e' lo script: non toccare nulla.
[ -f "$APPLY" ] || exit 0

# percent_margin non e scrivibile finche il boot non e completato.
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 120 ]; do
    i=$((i + 1))
    sleep 2
done
[ "$(getprop sys.boot_completed)" = "1" ] || exit 0

P=$(cat "$BASE/active" 2>/dev/null | tr -d ' \t\r\n')
[ -z "$P" ] && P="balanced"

# Riapplichiamo il profilo piu volte.
#
# Motivo: anche il modulo schedutil-uv2 scrive margini e cap poco dopo
# il boot, e i due script girano in parallelo. L'ultimo che scrive
# vince, quindi conviene passare dopo di lui. Dopo il primo passaggio
# il profilo e gia applicato: i successivi servono solo a superare
# quella race, e sono innocui perche il profilo e idempotente.
#
# Inoltre al boot pm_qos del vendor puo rifiutare una scrittura su
# scaling_max_freq (cap richiesto 2002000, ottenuto 1456000): il secondo
# passaggio lo riapplica.
for delay in 40 55 55; do
    sleep "$delay"
    sh "$APPLY" "$P" >/dev/null 2>&1
done

exit 0