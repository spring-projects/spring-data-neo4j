#!/bin/bash -x

set -euo pipefail

mkdir -p /tmp/jenkins-home

export JENKINS_USER=${JENKINS_USER_NAME}
export SDN_FORCE_REUSE_OF_CONTAINERS=true
export SDN_NEO4J_VERSION=5.20

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home -Dscan=false" \
  ./mvnw -s settings.xml -P${PROFILE} clean dependency:list verify -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-neo4j -Ddevelocity.storage.directory=/tmp/jenkins-home/.develocity-root
