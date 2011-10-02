package org.springframework.data.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

/**
 * @author mh
 * @since 30.09.11
 */
public class CrossStoreNodeEntityStateFactory extends NodeEntityStateFactory {
    private PartialNodeEntityState.PartialNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory;
    private EntityManagerFactory entityManagerFactory;

    public EntityState<Node> getEntityState(final NodeBacked entity) {
        final Class<?> entityType = entity.getClass();
        final NodeEntity graphEntityAnnotation = entityType.getAnnotation(NodeEntity.class); // todo cache ??
        final Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
        if (graphEntityAnnotation.partial()) {
            final PartialNodeEntityState<NodeBacked> partialNodeEntityState = new PartialNodeEntityState<NodeBacked>(null, entity, entityType, graphDatabaseContext, getPersistenceUnitUtils(), delegatingFieldAccessorFactory, (Neo4jPersistentEntity<NodeBacked>) persistentEntity);
            return new DetachedEntityState<Node>(partialNodeEntityState, graphDatabaseContext) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || partialNodeEntityState.getId(entity) == null;
                }
            };
        } else {
            NodeEntityState<NodeBacked> nodeEntityState = new NodeEntityState<NodeBacked>(null, entity, entityType, graphDatabaseContext, nodeDelegatingFieldAccessorFactory, (Neo4jPersistentEntity<NodeBacked>) persistentEntity);
            // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
            return new DetachedEntityState<Node>(nodeEntityState, graphDatabaseContext);
        }
    }

    private PersistenceUnitUtil getPersistenceUnitUtils() {
        if (entityManagerFactory == null) return null;
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    private void setUp() {
         this.delegatingFieldAccessorFactory = new PartialNodeEntityState.PartialNodeDelegatingFieldAccessorFactory(graphDatabaseContext);
    }

}
