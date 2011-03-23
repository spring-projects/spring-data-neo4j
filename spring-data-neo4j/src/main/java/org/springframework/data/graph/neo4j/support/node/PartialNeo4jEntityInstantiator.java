/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.fieldaccess.PartialNodeEntityState;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.persistence.support.EntityInstantiator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Entity instantiator for Node entities that takes into account that the entity is persisted in a JPA store as well.
 *
 * @author Michael Hunger
 * @since 02.10.2010
 */
public class PartialNeo4jEntityInstantiator implements EntityInstantiator<NodeBacked, Node> {

	private final Neo4jConstructorGraphEntityInstantiator delegate;
    private EntityManagerFactory entityManagerFactory;


    public PartialNeo4jEntityInstantiator(Neo4jConstructorGraphEntityInstantiator delegate, EntityManagerFactory entityManagerFactory) {
		this.delegate = delegate;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Takes the JPA id stored in the "FOREIGN_ID" property of the node for a {@link EntityManager#find(Class, Object)} operation.
     * If the entity is found its instance is associated with the graph node and returned otherwise a new node entity instance for
     * this node is created by the original {@link EntityInstantiator}.
     * @param n Node to instantiate an entity for
     * @param entityClass type of the entity
     * @param <T> generic type of the entity
     * @return
     */
	public <T extends NodeBacked> T createEntityFromState(Node n, Class<T> entityClass) {
        if (n.hasProperty(PartialNodeEntityState.FOREIGN_ID)) {
            final Object foreignId = n.getProperty(PartialNodeEntityState.FOREIGN_ID);
            final T result = entityManager().find(entityClass, foreignId);
            result.setPersistentState(n);
            return result;
        }
        return delegate.createEntityFromState(n, entityClass);
    }

    private EntityManager entityManager() {
        return EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
    }
}
