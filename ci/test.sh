#!/bin/bash -x

set -euo pipefail

# Checking for the major class versions seems more sane than fiddling around with the
# various formats the different JDKs offer
JAVA_VERSION=`javap -verbose java.lang.String | grep "major version" | cut -d " " -f5`
ADDITIONAL_JAVA_OPS=""

if [[ "$JAVA_VERSION" -ge 60 ]]
then
  # SDN 6 itself does not need this, but the embedded Neo4j database used by jQAssistant during verification
  # of SDN 6 requires it.
  ADDITIONAL_JAVA_OPS="--add-opens java.base/java.lang=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"
fi

mkdir -p /tmp/jenkins-home
chown -R 1001:1001 .
MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home $ADDITIONAL_JAVA_OPS" \
  ./mvnw -s settings.xml -P${PROFILE} clean dependency:list verify -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-neo4j
