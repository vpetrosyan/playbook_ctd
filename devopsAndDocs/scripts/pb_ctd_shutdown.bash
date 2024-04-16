#!/bin/bash

psOutput=$(ps -ef | grep playbook_ctd | grep -v grep)
processID=$(echo ${psOutput} | awk '{ print $2 }')
echo "Process id of playbook_ctd is: ${processID}"

kill -9 ${processID}

if [ $? -eq 0 ]; then
        echo "Playbook CTD API terminated!"
else
        echo "Unable to spot process playbook_ctd, shutdown the device or do it manualy using the ps and kill commands!"
fi

