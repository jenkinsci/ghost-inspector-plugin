#!/bin/bash

# Fetch Jenkins CLI
echo "Fetching Jenkins CLI"
cd /tmp
wget http://localhost:8080/jnlpJars/jenkins-cli.jar
chmod 744 jenkins-cli.jar

wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
mv ./jq-linux64 ./jq
chmod +x ./jq

# Trigger the job
echo "Triggering build for $JOB"

# Allow cli user to interact with jenkins
export JAVA_OPTS=-Dpermissive-script-security.enabled=no_security

java -jar jenkins-cli.jar -s http://localhost:8080/ build $JOB

STATUS='None'
echo "Polling for job result"
while [ "$STATUS" = 'None' ]; do
  sleep 5
  STATUS=$(curl -s "http://localhost:8080/job/$JOB/lastBuild/api/json" | ./jq '.result')
  echo " - status: $STATUS"
done

if [ "$STATUS" != 'SUCCESS' ]; then
  exit 1
else 
  exit 0
fi
