#!/bin/bash

# trigger the job
echo "Triggering build for $JOB"
java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar -s http://jenkins:8080/ build $JOB

STATUS='None'
echo "Polling for job result"
while [ "$STATUS" = 'None' ]; do
  sleep 5
  STATUS=$(curl -s "http://jenkins:8080/job/$JOB/lastBuild/api/json" | python -c 'import json; import sys; data=json.load(sys.stdin); print data.get("result")')
  echo " - status: $STATUS"
done

if [ "$STATUS" != 'SUCCESS' ]; then
  exit 1
else 
  exit 0
fi