/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc.", "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.config;

import org.springframework.data.neo4j.repository.support.GraphRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

/**
 * @author Vince Bickers
 */
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
