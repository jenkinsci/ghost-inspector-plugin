#!/bin/bash

# Fetch Jenkins CLI
echo "Fetching Jenkins CLI"
cd /tmp
wget http://localhost:8080/jnlpJars/jenkins-cli.jar
chmod 744 jenkins-cli.jar

wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
mv ./jq-linux64 ./jq
chmod +x ./jq

JENKINS_PASSWORD="0209e98d0c274a19b137afd682a1820d"

# Trigger the job
echo "Triggering build for $JOB"

java -jar jenkins-cli.jar -s http://localhost:8080/ -auth "admin:$JENKINS_PASSWORD" build $JOB

STATUS='null'
echo "Polling for job result"
while [ "$STATUS" = 'null' ]; do
  sleep 5
  RESULT=$(curl -s --user admin:$JENKINS_PASSWORD "http://localhost:8080/job/$JOB/lastBuild/api/json")
  echo " - result $RESULT"
  STATUS=$(echo $RESULT | ./jq '.result')
  echo " - status: $STATUS"
done

if [ "$STATUS" != 'SUCCESS' ]; then
  exit 1
else 
  exit 0
fi
