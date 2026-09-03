DELIMITER="-------------------------------------"

hostname -f &> /dev/null && printf "Hostname : $(hostname -f)\n" || printf "Hostname : $(hostname -s)\n"

echo -e "Kernel Version :" $(uname -r)
echo "OS Architecture : $(arch)"

uptime -p &> /dev/null && echo -en "System Uptime : " $(uptime -p) || echo -en "System Uptime : " $(uptime)
echo -e "\nCurrent System Date & Time : "$(date +%c)

echo -e "Total Swap Memory in MiB : "$(grep -w SwapTotal /proc/meminfo|awk '{print $2/1024}')", in GiB : "\
$(grep -w SwapTotal /proc/meminfo|awk '{print $2/1024/1024}')
echo -e "Swap Free Memory in MiB : "$(grep -w SwapFree /proc/meminfo|awk '{print $2/1024}')", in GiB : "\
$(grep -w SwapFree /proc/meminfo|awk '{print $2/1024/1024}')

echo -e "\n\nMost Recent 3 Reboots"
echo -e "$DELIMITER$DELIMITER"
last -x 2> /dev/null|grep reboot 1> /dev/null && /usr/bin/last -x 2> /dev/null|grep reboot|head -3 || \
echo -e "No reboot events are recorded."

echo -e "\n\nMost Recent 3 Shutdowns"
echo -e "$DELIMITER$DELIMITER"
last -x 2> /dev/null|grep shutdown 1> /dev/null && /usr/bin/last -x 2> /dev/null|grep shutdown|head -3 || \
echo -e "No shutdown events are recorded."
