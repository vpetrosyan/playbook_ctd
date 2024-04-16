#!/bin/bash

playbookCtdFileName=$(ls *.jar)
profile="dev"

nohup java -jar -Dspring.profiles.active=${profile} ${playbookCtdFileName} &
if [ $? -eq 0 ]; then
   echo "Starting ${playbookCtdFileName} jar, profile: ${profile}"
else
   echo "ERROR: Unable to start ${playbookCtdFileName} jar!"
fi
