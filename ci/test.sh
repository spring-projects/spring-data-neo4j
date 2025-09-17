#!/bin/bash -x
#
# Copyright 2011-2025 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


set -euo pipefail

mkdir -p /tmp/jenkins-home

export JENKINS_USER=${JENKINS_USER_NAME}
export SDN_FORCE_REUSE_OF_CONTAINERS=true
export SDN_NEO4J_VERSION=5.26.12

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home -Dscan=false" \
  ./mvnw -s settings.xml -P${PROFILE} clean dependency:list verify -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-neo4j -Ddevelocity.storage.directory=/tmp/jenkins-home/.develocity-root
