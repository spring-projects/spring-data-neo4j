#!/usr/bin/env bash

# This script is used to run SDN6's integration test against a Neo4j cluster via `runClusterTests.java`. The idea behind it is:
# 1. Build a local deployment of SDN 6 including a test jar containing all tests
# 2. Extract the version, direct dependencies and the repo path from the Maven build descriptor and replace the placeholders
#    in the template.
# 3. Execute the Java script file via JBang.
#
# The result code of this script will be 0 in a successful run, a non-zero value otherwise. The Java program will try
# upto 100 times to get a completely successful test run, retrying on error cases that might happen in a cluster.
#
# Run this script with a pair of environmental values to point it to a cluster:
# SDN_NEO4J_URL=neo4j+s://your.neo4j.cluster.io SDN_NEO4J_PASSWORD=yourPassword ./ci/runClusterTests.sh

set -euo pipefail

CI_BASEDIR=$(dirname "$0")
BASEDIR=$(realpath $CI_BASEDIR/..)
CLUSTER_TEST_DIR=$BASEDIR/target/cluster-tests

(
  cd $BASEDIR
  SDN_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

  mkdir -p $CLUSTER_TEST_DIR

  # Create the distribution and deploy it into the target folder itself
  ./mvnw -Pgenerate-test-jar -DskipTests clean deploy -DaltDeploymentRepository=snapshot-repo::default::file:///$CLUSTER_TEST_DIR/snapshot-repo

  # Massage the directory name into something sed is happy with
  SNAPSHOT_REPO=$(printf '%s\n' "$CLUSTER_TEST_DIR/snapshot-repo" | sed -e 's/[\/&]/\\&/g')

  # Create a plain list of dependencies
  ./mvnw dependency:list -DexcludeTransitive  | sed -n -e 's/^\[INFO\]    //p' > $CLUSTER_TEST_DIR/dependencies.txt

  # Update repository path, version and dependencies in template
  sed -e s/\$SDN_VERSION/$SDN_VERSION/ -e s/\$SNAPSHOT_REPO/$SNAPSHOT_REPO/ $CI_BASEDIR/runClusterTests.template.java |\
    awk -F: -v deps=$CLUSTER_TEST_DIR/dependencies.txt -v target=$CLUSTER_TEST_DIR/runClusterTests.java '
      /\/\/\$ADDITIONAL_DEPENDENCIES/ {
        while((getline < deps) > 0) {
          print "//DEPS "  $1 ":" $2 ":" $4 > target
        }
        next
      }
      {print > target}'

  # clean up
  rm $CLUSTER_TEST_DIR/dependencies.txt

  # Prepare run
  chmod +x $CLUSTER_TEST_DIR/runClusterTests.java && cp src/test/resources/logback-silent.xml $CLUSTER_TEST_DIR/logback.xml
)

jbang $CLUSTER_TEST_DIR/runClusterTests.java
