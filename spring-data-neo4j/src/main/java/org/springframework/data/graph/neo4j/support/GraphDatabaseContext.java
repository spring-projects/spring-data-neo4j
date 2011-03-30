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
import org.springframework.data.graph.core.*;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.validation.Validator;
import java.util.Map;

/**
 * Mediator class for the graph related services like the {@link GraphDatabaseService}, the used
 * {@link org.springframework.data.graph.core.TypeRepresentationStrategy}, entity instantiators for nodes and relationships as well as a spring conversion service.
 *
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class GraphDatabaseContext {

    private static final Log log = LogFactory.getLog(GraphDatabaseContext.class);
    public static final String DEFAULT_NODE_INDEX_NAME = "node";
    public static final String DEFAULT_RELATIONSHIP_INDEX_NAME = "relationship";

    private GraphDatabaseService graphDatabaseService;
    private ConversionService conversionService;

    private Validator validator;
    private NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    private RelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy;

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
    // TODO: What about connected relationship entities?
    public void removeNodeEntity(NodeBacked entity) {
        Node node = entity.getPersistentState();
        if (node==null) return;
        nodeTypeRepresentationStrategy.preEntityRemoval(entity);
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
     * delegates to the configured @{link TypeRepresentationStrategy} to iterate over all instances of this type
     * @param entityClass type of entity
     * @param <T>
     * @return
     * TODO inheritance handling
     */
    public <T extends GraphBacked> Iterable<T> findAll(final Class<T> entityClass) {
        return getTypeRepresentationStrategy(entityClass).findAll(entityClass);
    }

    /**
     * delegates to the configured @{link TypeRepresentationStrategy} for a count of all instances of this type
     * @param entityClass
     * @return count of all instances
     */
    public <T extends GraphBacked> long count(final Class<T> entityClass) {
        return getTypeRepresentationStrategy(entityClass).count(entityClass);
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



    public <S extends PropertyContainer, T extends GraphBacked<S>> T createEntityFromState(S state, Class<T> type) {
        if (state==null) throw new IllegalArgumentException("state has to be either a Node or Relationship, not null");
        return getTypeRepresentationStrategy(state, type).createEntity(state, type);
    }

    public <S extends PropertyContainer, T extends GraphBacked<S>> T projectTo(GraphBacked<S> entity, Class<T> targetType) {
        S state = entity.getPersistentState();
        return getTypeRepresentationStrategy(state, targetType).projectEntity(state, targetType);
    }

    public <T extends NodeBacked> T createEntityFromStoredType(Node node) {
        return nodeTypeRepresentationStrategy.createEntity(node);
    }

    public <S extends PropertyContainer, T extends GraphBacked<S>> void postEntityCreation(S node, Class<T> entityClass) {
        getTypeRepresentationStrategy(node, entityClass).postEntityCreation(node, entityClass);
    }



    @SuppressWarnings("unchecked")
    private <T extends GraphBacked<? extends PropertyContainer>>
        TypeRepresentationStrategy<?, T> getTypeRepresentationStrategy(Class<T> type) {
        if (NodeBacked.class.isAssignableFrom(type)) {
            return (TypeRepresentationStrategy<?, T>) nodeTypeRepresentationStrategy;
        } else if (RelationshipBacked.class.isAssignableFrom(type)) {
            return (TypeRepresentationStrategy<?, T>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not NodeBacked nor RelationshipBacked.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T extends GraphBacked<S>>
        TypeRepresentationStrategy<S, T> getTypeRepresentationStrategy(S state, Class<T> type) {
        if (state instanceof Node && NodeBacked.class.isAssignableFrom(type)) {
            return (TypeRepresentationStrategy<S, T>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship && RelationshipBacked.class.isAssignableFrom(type)) {
            return (TypeRepresentationStrategy<S, T>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not NodeBacked nor RelationshipBacked.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T extends GraphBacked<S>>
        TypeRepresentationStrategy<S, T> getTypeRepresentationStrategy(S state) {
        if (state instanceof Node) {
            return (TypeRepresentationStrategy<S, T>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship) {
            return (TypeRepresentationStrategy<S, T>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not NodeBacked nor RelationshipBacked.");
    }



    /**
     * @return Neo4j Transaction manager
     */
    public TransactionManager getTxManager() {
        return ((AbstractGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Node createNode() {
        return graphDatabaseService.createNode();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Node getNodeById(final long nodeId) {
        return graphDatabaseService.getNodeById(nodeId);
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
	public Node getReferenceNode() {
        return graphDatabaseService.getReferenceNode();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Iterable<? extends Node> getAllNodes() {
        return graphDatabaseService.getAllNodes();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Transaction beginTx() {
        return graphDatabaseService.beginTx();
    }

    /**
     * Delegates to {@link GraphDatabaseService}
     */
    public Relationship getRelationshipById(final long id) {
        return graphDatabaseService.getRelationshipById(id);
    }



    public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

    public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public void setNodeTypeRepresentationStrategy(NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy) {
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
    }

    public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    public void setRelationshipTypeRepresentationStrategy(RelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy) {
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
    }

    public ConversionService getConversionService() {
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validatorFactory) {
        this.validator = validatorFactory;
    }



}

