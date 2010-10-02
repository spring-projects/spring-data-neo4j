package org.springframework.datastore.graph.neo4j.spi.node;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.fieldaccess.PartialNodeEntityStateAccessors;
import org.springframework.persistence.support.EntityInstantiator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 */
public class PartialNeo4jEntityInstantiator implements EntityInstantiator<NodeBacked, Node> {
    Neo4jConstructorGraphEntityInstantiator delegate;
    EntityManager entityManager;

    public <T extends NodeBacked> T createEntityFromState(Node n, Class<T> entityClass) {
        if (n.hasProperty(PartialNodeEntityStateAccessors.FOREIGN_ID)) {
            final Object foreignId = n.getProperty(PartialNodeEntityStateAccessors.FOREIGN_ID);
            final T result = entityManager.find(entityClass, foreignId);
            result.setUnderlyingState(n);
            return result;
        }
        return delegate.createEntityFromState(n, entityClass);
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public void setEntityInstantiator(Neo4jConstructorGraphEntityInstantiator delegate) {
        this.delegate = delegate;
    }
}
