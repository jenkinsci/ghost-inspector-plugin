FROM jenkins/jenkins:lts

COPY jenkins_home /var/jenkins_home
COPY scripts /tmp/scripts
