/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactoryBean<S extends PropertyContainer, R extends CRUDGraphRepository<T>, T extends GraphBacked<S>>
        extends TransactionalRepositoryFactoryBeanSupport<R, T, Long> {

    private GraphDatabaseContext graphDatabaseContext;

    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }


    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {

        return createRepositoryFactory(graphDatabaseContext);
    }


    protected RepositoryFactorySupport createRepositoryFactory(GraphDatabaseContext graphDatabaseContext) {

        return new GraphRepositoryFactory(graphDatabaseContext);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(graphDatabaseContext, "GraphDatabaseContext must not be null!");
        super.afterPropertiesSet();
    }
}