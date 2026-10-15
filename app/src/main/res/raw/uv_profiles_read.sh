#!/system/bin/sh
# Lettura dei valori di sistema per il pannello STATUS.
# Eseguito dall'app come root: /sys/power/percent_margin ha permessi 600.
#
# Separatore dei campi: '@'. La shell interpreta '|' come pipe anche dentro
# $(), e ':' rendeva fragile lo split sui valori negativi dei margini.

P=/sys/devices/system/cpu/cpufreq
M=/sys/power/percent_margin

echo "PROFILE@$(cat /data/adb/uv-profiles/active 2>/dev/null | tr -d ' \r\n')"
echo "LIT@$(cat $P/policy0/scaling_cur_freq 2>/dev/null)@$(cat $P/policy0/scaling_max_freq 2>/dev/null)@$(cat $P/policy0/cpuinfo_max_freq 2>/dev/null)"
echo "MID@$(cat $P/policy4/scaling_cur_freq 2>/dev/null)@$(cat $P/policy4/scaling_max_freq 2>/dev/null)@$(cat $P/policy4/cpuinfo_max_freq 2>/dev/null)"
echo "PRI@$(cat $P/policy6/scaling_cur_freq 2>/dev/null)@$(cat $P/policy6/scaling_max_freq 2>/dev/null)@$(cat $P/policy6/cpuinfo_max_freq 2>/dev/null)"

n=0
for c in 0 1 2 3 4 5 6 7; do
  [ "$(cat /sys/devices/system/cpu/cpu$c/online 2>/dev/null)" = 1 ] && n=$((n+1))
done
echo "CORES@$n"

echo "MLIT@$(cat $M/lit_margin_percent 2>/dev/null)"
echo "MMID@$(cat $M/mid_margin_percent 2>/dev/null)"
echo "MBIG@$(cat $M/big_margin_percent 2>/dev/null)"
echo "MG3D@$(cat $M/g3d_margin_percent 2>/dev/null)"
echo "MDISP@$(cat $M/disp_margin_percent 2>/dev/null)"
echo "MMIF@$(cat $M/mif_margin_percent 2>/dev/null)"
echo "MNPU@$(cat $M/npu_margin_percent 2>/dev/null)"

echo "LOAD@$(cut -d' ' -f1-3 /proc/loadavg)"

t=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null)
echo "TEMP@$((t/1000))"

echo "MEM@$(grep MemAvailable /proc/meminfo | tr -s ' ' | cut -d' ' -f2)"
echo "SWAP@$(grep SwapFree /proc/meminfo | tr -s ' ' | cut -d' ' -f2)"