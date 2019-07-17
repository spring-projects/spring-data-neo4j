# ---
# Cache most of the dependencies
# ---
FROM maven:3.6-jdk-12 AS maven_base
COPY . /tmp/sdn-rx
WORKDIR /tmp/sdn-rx
# Can only resolve the dependencies of sdn-rx itself, as the starter is dependent on it and
# would not find the freshly build version in the reactor
RUN mvn dependency:resolve -pl 'org.neo4j.springframework.data:spring-data-neo4j-rx'

# ---
# Build and install sdn-rx
# ---
FROM maven:3.6-jdk-12 AS sdn_rx_build
COPY --from=maven_base /root/.m2 /root/.m2
COPY --from=maven_base /tmp/sdn-rx /tmp/sdn-rx
WORKDIR /tmp/sdn-rx
RUN mvn -DskipTests clean install -pl '!org.neo4j.doc.springframework.data:spring-data-neo4j-rx-examples-reactive-web,!org.neo4j.doc.springframework.data:spring-data-neo4j-rx-examples-mapping'

# ---
# Create the actual container for the examples
# ---
FROM maven:3.6-jdk-12

# Copy over the build
COPY --from=sdn_rx_build /root/.m2 /root/.m2

# Install some more tools
RUN yum install git -y

# Allow for a consistant java home location for settings - image is changing over time
RUN if [ ! -d "/docker-java-home" ]; then ln -s "${JAVA_HOME}" /docker-java-home; fi

# Set the default shell to bash rather than sh
ENV SHELL /bin/bash

# Make it possible to use a locally running database.
RUN yum install iputils -y
COPY examples/reactive-web/.devcontainer/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["mvn"]
