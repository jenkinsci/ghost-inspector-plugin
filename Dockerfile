FROM jenkins/jenkins:lts

COPY jenkins_home --chown=jenkins /var/jenkins_home
COPY scripts --chown=jenkins /tmp/scripts
