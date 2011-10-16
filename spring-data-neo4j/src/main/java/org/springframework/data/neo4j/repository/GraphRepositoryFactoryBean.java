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
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
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
    private MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

    public void setNeo4jTemplate(Neo4jTemplate template) {
        this.template = template;
    }

    /**
     * @param mappingContext the mappingContext to set
     */
    public void setMappingContext(
            MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return createRepositoryFactory(template);
    }

    protected RepositoryFactorySupport createRepositoryFactory(Neo4jTemplate template) {

        return new GraphRepositoryFactory(template, mappingContext);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(template, "Neo4jTemplate must not be null!");

        if (mappingContext == null) {
            Neo4jMappingContext context = new Neo4jMappingContext();
            context.afterPropertiesSet();
            this.mappingContext = context;
        }

        super.afterPropertiesSet();
    }
}