/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;


/**
 * @author Vince Bickers
 */
public class GraphRepositoryFactoryBean<S extends Repository<T, Long>, T> extends TransactionalRepositoryFactoryBeanSupport<S, T, Long> {

    @Autowired
    private Session session;

    @Autowired
    private Neo4jMappingContext mappingContext;
    
    @Override
    public void afterPropertiesSet() {
        setMappingContext(mappingContext);
        super.afterPropertiesSet();
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new GraphRepositoryFactory(session);
    }
}
