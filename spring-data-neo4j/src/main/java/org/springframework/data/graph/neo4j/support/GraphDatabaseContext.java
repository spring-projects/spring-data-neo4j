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

package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.persistence.EntityInstantiator;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.validation.Validator;
import java.util.Map;

/**
 * Mediator class for the graph related services like the {@link GraphDatabaseService}, the used
 * {@link NodeTypeStrategy}, entity instantiators for nodes and relationships as well as a spring conversion service.
 *
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class GraphDatabaseContext {

    public static final String DEFAULT_NODE_INDEX_NAME = "node";
    public static final String DEFAULT_RELATIONSHIP_INDEX_NAME = "relationship";

    private GraphDatabaseService graphDatabaseService;

    public EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    public EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

    private ConversionService conversionService;

    private NodeTypeStrategy nodeTypeStrategy;

    private Validator validator;

    private final static Log log = LogFactory.getLog(GraphDatabaseContext.class);

    public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

	public EntityInstantiator<NodeBacked, Node> getGraphEntityInstantiator() {
		return graphEntityInstantiator;
	}

	public void setGraphEntityInstantiator(
			EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.graphEntityInstantiator = graphEntityInstantiator;
	}

	public EntityInstantiator<RelationshipBacked, Relationship> getRelationshipEntityInstantiator() {
		return relationshipEntityInstantiator;
	}

	public void setRelationshipEntityInstantiator(
			EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.relationshipEntityInstantiator = relationshipEntityInstantiator;
	}

	public ConversionService getConversionService() {
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public NodeTypeStrategy getNodeTypeStrategy() {
		return nodeTypeStrategy;
	}

	public void setNodeTypeStrategy(NodeTypeStrategy nodeTypeStrategy) {
		this.nodeTypeStrategy = nodeTypeStrategy;
	}

	public Node createNode() {
        return graphDatabaseService.createNode();
    }

    /**
     * @param relationship to remove from indexes and to delete
     */
    private void removeRelationship(Relationship relationship) {
        removeFromIndexes(relationship);
        relationship.delete();
    }

    /**
     * @param relationship to be removed from all indexes, all properties are removed from all indexes
     */
    private void removeFromIndexes(Relationship relationship) {
        IndexManager indexManager = graphDatabaseService.index();
        for (String indexName : getIndexManager().relationshipIndexNames()) {
            indexManager.forRelationships(indexName).remove(relationship);
        }
    }

    /**
     * removes the entity by cleaning the relationships first and then removing the node
     * it removes all of them from all indexes in advance
     * the entity and relationship are still accessible after removal but before transaction commit
     * but all modifications will throw an exception
     * @param entity to remove
     */
    public void removeNodeEntity(NodeBacked entity) {
        Node node = entity.getPersistentState();
        if (node==null) return;
        this.nodeTypeStrategy.preEntityRemoval(entity);
        for (Relationship relationship : node.getRelationships()) {
            removeRelationship(relationship);
        }
        removeFromIndexes(node);
        node.delete();
    }
    public void removeRelationshipEntity(RelationshipBacked entity) {
        Relationship relationship = entity.getPersistentState();
        if (relationship==null) return;
        removeRelationship(relationship);
    }

    /**
     * @param node to be removed from all indexes, all properties of the node are removed from all indexes
     */
    private void removeFromIndexes(Node node) {
        IndexManager indexManager = graphDatabaseService.index();
        for (String indexName : getIndexManager().nodeIndexNames()) {
            indexManager.forNodes(indexName).remove(node);
        }
    }

    /**
     * Creates either a node or relationship entity by delegating the creation to the appropriate @{link EntityInstantiator}
     * @param state Node or Relationship
     * @param type target entity type
     * @return an instance of the entity type
     */
    public <S, T extends GraphBacked> T createEntityFromState(final S state, final Class<T> type) {
        if (state==null) throw new IllegalArgumentException("state has to be either a Node or Relationship, not null");
        if (state instanceof Node)
            return (T) graphEntityInstantiator.createEntityFromState((Node) state, nodeTypeStrategy.confirmType((Node)state, (Class<? extends NodeBacked>)type));
        else
            return (T) relationshipEntityInstantiator.createEntityFromState((Relationship) state, (Class<? extends RelationshipBacked>) type);
    }

    private IndexManager getIndexManager() {
        return graphDatabaseService.index();
    }

    /**
     * @param indexName or null, "node" is assumed if null
     * @return node index {@link Index}
     */
    public <T extends PropertyContainer,N extends GraphBacked<T>> Index<T> getIndex(Class<N> type, String indexName) {
        if (indexName==null) indexName = Indexed.Name.get(type);
        if (NodeBacked.class.isAssignableFrom(type)) return (Index<T>) getIndexManager().forNodes(indexName);
        if (RelationshipBacked.class.isAssignableFrom(type)) return (Index<T>) getIndexManager().forRelationships(indexName);
        throw new IllegalArgumentException("Wrong index type supplied "+type);
    }
    /**
     * @param type type of index requested - either Node.class or Relationship.class
     * @param indexName or null, "node" is assumed if null
     * @param fullText true if a fulltext queryable index is needed, false for exact match
     * @return node index {@link Index}
     */
    public <T extends PropertyContainer,N extends GraphBacked<T>> Index<T> getIndex(Class<N> type, String indexName, boolean fullText) {
        if (indexName==null) indexName = Indexed.Name.get(type);
        Map<String, String> config = fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;
        if (NodeBacked.class.isAssignableFrom(type)) return (Index<T>) getIndexManager().forNodes(indexName, config);
        if (RelationshipBacked.class.isAssignableFrom(type)) return (Index<T>) getIndexManager().forRelationships(indexName, config);
        throw new IllegalArgumentException("Wrong index type supplied "+type);
    }

    /**
     * @param nodeId
     * @return Node
     * @throws NotFoundException
     */
    public Node getNodeById(final long nodeId) {
        return graphDatabaseService.getNodeById(nodeId);
    }

    /**
     * delegates to the configured @{link NodeTypeStrategy} for after entity creation operations
     * @param entity
     */
    public void postEntityCreation(final NodeBacked entity) {
        nodeTypeStrategy.postEntityCreation(entity);
    }

    /**
     * delegates to the configured @{link NodeTypeStrategy} to iterate over all instances of this type
     * @param clazz type of entity
     * @param <T>
     * @return
     * TODO inheritance handling
     */
    public <T extends GraphBacked> Iterable<T> findAll(final Class<T> clazz) {
        if (!checkIsNodeBacked(clazz)) throw new UnsupportedOperationException("No support for relationships");
        return (Iterable<T>) nodeTypeStrategy.findAll((Class<NodeBacked>)clazz);
    }

    /**
     * class base check for nodebacked subclasses
     */
    private boolean checkIsNodeBacked(Class<?> clazz) {
        return NodeBacked.class.isAssignableFrom(clazz);
    }

    /**
     * delegates to the configured @{link NodeTypeStrategy} for a count of all instances of this type
     * @param entityClass
     * @return count of all instances
     */
    public long count(final Class<? extends GraphBacked> entityClass) {
        if (!checkIsNodeBacked(entityClass)) throw new UnsupportedOperationException("No support for relationships");
        return nodeTypeStrategy.count((Class<NodeBacked>)entityClass);
    }

    /**
     * delegates to the configured @{link NodeTypeStrategy} to lookup the type information for the given node
     * @param node
     * @param <T>
     * @return entity type of the node
     * @throws IllegalStateException for nodes that are not instance backing nodes of a known type
     */
	public <T extends NodeBacked> Class<T> getJavaType(final Node node) {
		return nodeTypeStrategy.getJavaType(node);
	}

    /**
     * @return reference node of the graph database
     */
	public Node getReferenceNode() {
        return graphDatabaseService.getReferenceNode();
    }


    /**
     * @return Neo4j Transaction manager
     */
    public TransactionManager getTxManager() {

        return ((AbstractGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
    }

    /**
     * @return true if a transaction manager is available and a transaction is currently running
     */
    public boolean transactionIsRunning() {
        try {
            return getTxManager().getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            log.error("Error accessing TransactionManager", e);
            return false;
        }
    }

    /**
     * delegates to @{link GraphDatabaseService}
     */
    public Iterable<? extends Node> getAllNodes() {
        return graphDatabaseService.getAllNodes();
    }

    /**
     * delegates to @{link GraphDatabaseService}
     */
    public Transaction beginTx() {
        return graphDatabaseService.beginTx();
    }

    /**
     * delegates to @{link GraphDatabaseService}
     */
    public Relationship getRelationshipById(final long id) {
        return graphDatabaseService.getRelationshipById(id);
    }

    public <T extends GraphBacked> T projectTo(GraphBacked entity, Class<T> targetType) {
        final Object state = entity.getPersistentState();
        if (state instanceof Node)
            return (T) graphEntityInstantiator.createEntityFromState((Node) state, (Class<? extends NodeBacked>) targetType);
        else
            return (T) relationshipEntityInstantiator.createEntityFromState((Relationship) state, (Class<? extends RelationshipBacked>) targetType);
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validatorFactory) {
        this.validator = validatorFactory;
    }

    public <T extends NodeBacked> T createEntityFromStoredType(Node node) {
        return (T)graphEntityInstantiator.createEntityFromState(node,nodeTypeStrategy.<NodeBacked>getJavaType(node));
    }
}

