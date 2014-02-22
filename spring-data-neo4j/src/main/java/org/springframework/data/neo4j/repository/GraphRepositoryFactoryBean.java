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

package org.springframework.data.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactoryBean<S extends PropertyContainer, R extends CRUDRepository<T>, T> extends
TransactionalRepositoryFactoryBeanSupport<R, T, Long> {

    private Neo4jTemplate template;
    private Neo4jMappingContext neo4jMappingContext;

    public void setNeo4jTemplate(Neo4jTemplate template) {
        this.template = template;
    }

    /**
     * @param neo4jMappingContext the mappingContext to set
     */
    public void setNeo4jMappingContext(
            Neo4jMappingContext neo4jMappingContext) {
        this.neo4jMappingContext = neo4jMappingContext;
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return createRepositoryFactory(template);
    }

    protected RepositoryFactorySupport createRepositoryFactory(Neo4jTemplate template) {

        return new GraphRepositoryFactory(template, neo4jMappingContext);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(template, "Neo4jTemplate must not be null!");

        if (neo4jMappingContext == null) {
            Neo4jMappingContext context = new Neo4jMappingContext();
            context.initialize();
            this.neo4jMappingContext = context;
        }
	      setMappingContext(neo4jMappingContext);

        super.afterPropertiesSet();
    }
}
