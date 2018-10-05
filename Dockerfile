FROM jenkins/jenkins:lts

COPY --chown=jenkins jenkins_home /var/jenkins_home
COPY --chown=jenkins scripts /tmp/scripts
