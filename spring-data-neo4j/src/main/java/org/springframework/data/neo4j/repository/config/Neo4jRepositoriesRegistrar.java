package org.springframework.data.neo4j.repository.config;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

public class Neo4jRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableNeo4jRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new Neo4jRepositoryConfigurationExtension();
    }
}
