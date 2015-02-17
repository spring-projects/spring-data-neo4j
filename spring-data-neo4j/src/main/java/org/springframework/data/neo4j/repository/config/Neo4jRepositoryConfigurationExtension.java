package org.springframework.data.neo4j.repository.config;

import org.springframework.data.neo4j.repository.support.GraphRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

public class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getRepositoryFactoryClassName() {
        return GraphRepositoryFactoryBean.class.getName();
    }

    @Override
    protected String getModulePrefix() {
        return "neo4j";
    }

}
