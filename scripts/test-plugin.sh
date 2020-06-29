#!/bin/bash

# Fetch Jenkins CLI
echo "Fetching Jenkins CLI"
cd /tmp
wget http://localhost:8080/jnlpJars/jenkins-cli.jar
chmod 744 jenkins-cli.jar

# Trigger the job
echo "Triggering build for $JOB"
java -jar jenkins-cli.jar -s http://localhost:8080/ build $JOB

STATUS='None'
echo "Polling for job result"
while [ "$STATUS" = 'None' ]; do
  sleep 5
  STATUS=$(curl -s "http://localhost:8080/job/$JOB/lastBuild/api/json" | python -c 'import json; import sys; data=json.load(sys.stdin); print data.get("result")')
  echo " - status: $STATUS"
done

if [ "$STATUS" != 'SUCCESS' ]; then
  exit 1
else 
  exit 0
fi
