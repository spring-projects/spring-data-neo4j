#!/bin/bash -x

set -euo pipefail

mkdir -p /tmp/jenkins-home
chown -R 1001:1001 .

export DEVELOCITY_CACHE_USERNAME=${DEVELOCITY_CACHE_USR}
export DEVELOCITY_CACHE_PASSWORD=${DEVELOCITY_CACHE_PSW}
export JENKINS_USER=${JENKINS_USER_NAME}
export SDN_FORCE_REUSE_OF_CONTAINERS=true

# The environment variable to configure access key is still GRADLE_ENTERPRISE_ACCESS_KEY
export GRADLE_ENTERPRISE_ACCESS_KEY=${DEVELOCITY_ACCESS_KEY}

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home -Dscan=false" \
  ./mvnw -s settings.xml -P${PROFILE} clean dependency:list verify -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-neo4j
