FROM maven:3.6-jdk-12

# Install some more tools
RUN yum install git -y

# Allow for a consistant java home location for settings - image is changing over time
RUN if [ ! -d "/docker-java-home" ]; then ln -s "${JAVA_HOME}" /docker-java-home; fi

# Set the default shell to bash rather than sh
ENV SHELL /bin/bash

# Make it possible to use a locally running database.
RUN yum install iputils -y
COPY examples/rest/.devcontainer/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["mvn"]
