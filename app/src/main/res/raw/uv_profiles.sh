#!/system/bin/sh
# UV Profiles - applica un profilo. Script autonomo: vive nell'app e non
# dipende da alcun modulo Magisk.
#
#   sh uv_profiles.sh eco|balanced|sixcore|sport|off
#
# Percorso: /data/adb/uv-profiles (persistente, sopravvive al reboot e
# non viene cancellato come /data/local/tmp).

BASE=/data/adb/uv-profiles
LOG="$BASE/uv.log"
ACTIVE="$BASE/active"

mkdir -p "$BASE"

log() { echo "$(date '+%H:%M:%S') $*" >> "$LOG"; }

# ---- CONFIGURAZIONE ----
# formato: nome|lit|mid|big|g3d|disp|mif|npu|max_little|max_middle|max_prime|core_mask
#
# blocchi:
#   lit/mid/big  cluster CPU LITTLE (cpu0-3) / MIDDLE (cpu4,5) / PRIME (cpu6,7)
#   g3d          dominio GPU (Mali)
#   disp         dominio display (controlla la GPU sotto carico video)
#   mif          bus di memoria (nutre la GPU: spesso il vero collo di bottiglia)
#   npu          acceleratore neurale
#   max_*        cap del cluster. 0 = massimo hardware
#   core_mask    core da spegnere (online=0). Vuoto = nessuno.
#
# topologia reale di questo device (Exynos 990), verificata:
#   cpu0-3  Cortex-A55  0.442-2.002 GHz   policy0
#   cpu4,5  Cortex-A75  0.507-2.504 GHz   policy4
#   cpu6,7  Cortex-X1   0.546-2.730 GHz   policy6
#
# i margin score/iva/fsys0 sono esclusi: il kernel li rifiuta. Le tensioni
# assolute GPU (supply-0/u_volt_*) non sono utilizzabili: su questo kernel
# sono in sola lettura e valorizzate a 0.
#
# i cap sono per-policy e non per singolo core perche i tre cluster hanno
# scale OPP diverse: ogni valore deve essere un OPP valido per la sua policy.
#
#                lit  mid  big  g3d  disp  mif  npu   little    middle   prime     mask
ECO="eco|-6|-6|-7|-8|-6|-4|-4|1800000|1794000|2002000|"
BALANCED="balanced|-4|-4|-5|-6|-4|-2|-2|2002000|2002000|2002000|"
SPORT="sport|-2|-2|-3|-4|0|0|0|0|0|0|"
OFF="off|0|0|0|0|0|0|0|0|0|0|"
# MIDDLE ridotto a 1.794 GHz e PRIME bloccato a 1.157 GHz: il cluster piu
# veloce resta utilizzabile per i burst brevi ma non scala in continuazione.
# Nessun core spento: restano tutti online, quindi niente rischio di core
# offline e niente costo per lo scheduler.
SIXCORE="sixcore|-3|-4|0|-5|-4|-2|-2|2002000|1794000|1157000|"

get_profile() {
    case "$1" in
        eco|Eco|ECO)                echo "$ECO" ;;
        balanced|Balanced|BALANCED) echo "$BALANCED" ;;
        sport|Sport|SPORT)          echo "$SPORT" ;;
        sixcore|Sixcore|SIXCORE)    echo "$SIXCORE" ;;
        off|Off|OFF)                echo "$OFF" ;;
        *) echo "" ;;
    esac
}

# Salva i valori di fabbrica, una volta sola. Serve al profilo OFF per
# ripristinare: su questo kernel lo 0 NON e il default (lit e -3), quindi
# azzerare non riporterebbe il dispositivo allo stato originale.
#
# Se manca il file ma un profilo e gia attivo, i valori che trovo sono
# quelli modificati, non quelli di fabbrica: non li salvo e lo segnalo,
# altrimenti OFF "ripristinerebbe" uno stato gia alterato.
save_factory() {
    [ -f "$BASE/factory_margins.txt" ] && return 0
    if [ -f "$ACTIVE" ]; then
        log "ATTENZIONE: valori di fabbrica non salvati e profilo gia attivo"
        log "           OFF non puo ripristinarli, riavvia e rilancia l'app"
        return 1
    fi
    {
        for m in lit mid big g3d disp mif npu; do
            echo "$m=$(cat /sys/power/percent_margin/${m}_margin_percent 2>/dev/null)"
        done
    } > "$BASE/factory_margins.txt"
}

set_margin() {
    f="/sys/power/percent_margin/${1}_margin_percent"
    [ -f "$f" ] || return 1
    echo "$2" > "$f" 2>/dev/null
    [ "$(cat "$f" 2>/dev/null)" = "$2" ]
}

# Cap di una policy, agganciato al suo range valido.
set_policy_cap() {
    pol="$1"
    want="$2"
    d="/sys/devices/system/cpu/cpufreq/policy$pol"
    [ -f "$d/scaling_max_freq" ] || return 1
    imax=$(cat "$d/cpuinfo_max_freq" 2>/dev/null)
    imin=$(cat "$d/scaling_min_freq" 2>/dev/null)
    if [ "$want" = "0" ] || [ -z "$want" ]; then
        target="$imax"
    else
        target="$want"
        # non superare il massimo hardware
        if [ -n "$imax" ] && [ "$imax" -lt "$want" ] 2>/dev/null; then
            target="$imax"
        fi
        # non scendere sotto il minimo della policy
        if [ -n "$imin" ] && [ "$target" -lt "$imin" ] 2>/dev/null; then
            target="$imin"
        fi
    fi
    [ -z "$target" ] && return 1
    echo "$target" > "$d/scaling_max_freq" 2>/dev/null
    got=$(cat "$d/scaling_max_freq" 2>/dev/null)
    if [ "$got" = "$target" ]; then
        log "  policy$pol cap -> $got (ok)"
        return 0
    fi
    log "  policy$pol cap: richiesto $target, ottenuto $got"
    return 1
}

# Riaccende i core che un profilo precedente aveva spento. Va fatto prima
# di toccare i cap: su un core spento scaling_max_freq non e scrivibile.
wake_all_cores() {
    [ -f "$BASE/sixcore_state.txt" ] || return 0
    for c in $(cat "$BASE/sixcore_state.txt" 2>/dev/null); do
        f="/sys/devices/system/cpu/cpu$c/online"
        [ -f "$f" ] && { echo 1 > "$f" 2>/dev/null; log "  cpu$c riacceso"; }
    done
    rm -f "$BASE/sixcore_state.txt"
    sleep 1
}

apply() {
    name="$1"
    p="$(get_profile "$name")"
    [ -z "$p" ] && return 1

    n=$(echo "$p" | cut -d'|' -f1)
    lit=$(echo "$p" | cut -d'|' -f2)
    mid=$(echo "$p" | cut -d'|' -f3)
    big=$(echo "$p" | cut -d'|' -f4)
    g3d=$(echo "$p" | cut -d'|' -f5)
    disp=$(echo "$p" | cut -d'|' -f6)
    mif=$(echo "$p" | cut -d'|' -f7)
    npu=$(echo "$p" | cut -d'|' -f8)
    max_lit=$(echo "$p" | cut -d'|' -f9)
    max_mid=$(echo "$p" | cut -d'|' -f10)
    max_pri=$(echo "$p" | cut -d'|' -f11)
    core_mask=$(echo "$p" | cut -d'|' -f12)

    save_factory
    log "applico profilo: $n"

    if [ "$n" = "off" ]; then
        for m in lit mid big g3d disp mif npu; do
            fv=$(grep "^$m=" "$BASE/factory_margins.txt" 2>/dev/null | cut -d= -f2)
            [ -n "$fv" ] && set_margin "$m" "$fv"
        done
        wake_all_cores

        # ripristina i cap per policy, non per singolo core
        for pol in 0 4 6; do
            d="/sys/devices/system/cpu/cpufreq/policy$pol"
            [ -f "$d/scaling_max_freq" ] || continue
            fv=$(cat "$d/cpuinfo_max_freq" 2>/dev/null)
            [ -n "$fv" ] && echo "$fv" > "$d/scaling_max_freq" 2>/dev/null
        done
        log "profilo off: valori di fabbrica ripristinati"
        echo "$n" > "$ACTIVE"
        return 0
    fi

    set_margin lit "$lit"
    set_margin mid "$mid"
    set_margin big "$big"
    set_margin g3d "$g3d"
    set_margin disp "$disp"
    set_margin mif "$mif"
    set_margin npu "$npu"

    set_policy_cap 0 "$max_lit"
    set_policy_cap 4 "$max_mid"
    set_policy_cap 6 "$max_pri"

    wake_all_cores

    if [ -n "$core_mask" ]; then
        echo "$core_mask" > "$BASE/sixcore_state.txt"
        sleep 1
        for c in $core_mask; do
            f="/sys/devices/system/cpu/cpu$c/online"
            if [ -f "$f" ]; then
                if echo 0 > "$f" 2>/dev/null && [ "$(cat "$f" 2>/dev/null)" = "0" ]; then
                    log "  cpu$c SPENTO"
                else
                    log "  cpu$c non spento: kernel ha rifiutato"
                fi
            fi
        done
    fi

    # governor schedutil con rate limit 5000, se esposto
    for c in 0 1 2 3 4 5 6 7; do
        f="/sys/devices/system/cpu/cpu$c/online"
        # salta i core spenti: su quelli le scritture falliscono
        [ -f "$f" ] && [ "$(cat "$f" 2>/dev/null)" = "0" ] && continue
        d=/sys/devices/system/cpu/cpu$c/cpufreq
        [ -f "$d/scaling_governor" ] || continue
        avail=$(cat "$d/scaling_available_governors" 2>/dev/null)
        case " $avail " in
            *" schedutil "*) echo schedutil > "$d/scaling_governor" 2>/dev/null ;;
        esac
        if [ -f "$d/schedutil/up_rate_limit_us" ]; then
            echo 5000 > "$d/schedutil/up_rate_limit_us" 2>/dev/null
            echo 5000 > "$d/schedutil/down_rate_limit_us" 2>/dev/null
        fi
    done

    echo "$n" > "$ACTIVE"
    log "profilo attivo: $n"
    return 0
}

case "$1" in
    eco|balanced|sport|sixcore|off|Eco|Balanced|Sport|Sixcore|Off|ECO|BALANCED|SPORT|SIXCORE|OFF)
        if apply "$1"; then
            echo "profilo applicato: $1"
        else
            echo "profilo non valido: $1"
            exit 1
        fi
        ;;
    *)
        echo "uso: uv_profiles.sh {eco|balanced|sixcore|sport|off}"
        exit 1
        ;;
esac
exit 0