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
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.persistence.support.EntityInstantiator;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * Mediator class for the graph related services like the {@link GraphDatabaseService}, the {@link IndexService} the used
 * {@link NodeTypeStrategy}, entity instantiators for nodes and relationships as well as a spring conversion service.
 *
 * It delegates the appropriate methods to those services. The services are not intended to be accessible from outside.
 * TODO constructor injection
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class GraphDatabaseContext {

    private GraphDatabaseService graphDatabaseService;

    public EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    public EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

    private IndexManager indexManager;

    private ConversionService conversionService;

    private NodeTypeStrategy nodeTypeStrategy;
    
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

    public <S, T> T createEntityFromState(final S state, final Class<T> type) {
        if (state instanceof Node)
            return (T) graphEntityInstantiator.createEntityFromState((Node) state, getJavaType((Node) state));
        else
            return (T) relationshipEntityInstantiator.createEntityFromState((Relationship) state, (Class<? extends RelationshipBacked>) type);
    }

    private IndexManager getIndexManager() {
        return graphDatabaseService.index();
    }
    public IndexHits<Node> getIndexedNodes(final String property, final Object value) {
        return getNodeIndex().get(property, value.toString());
    }

    private Index<Node> getNodeIndex() {
        return getIndexManager().forNodes("node");
    }

    public Node getSingleIndexedNode(final String property, final Object value) {
        IndexHits<Node> indexHits = getIndexedNodes(property, value);
        return indexHits.hasNext() ? indexHits.next() : null;
    }

    public Node getNodeById(final long id) {
        return graphDatabaseService.getNodeById(id);
    }

    public void postEntityCreation(final NodeBacked entity) {
        nodeTypeStrategy.postEntityCreation(entity);
    }

    public <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz) {
        return nodeTypeStrategy.findAll(clazz);
    }

    public long count(final Class<? extends NodeBacked> entityClass) {
        return nodeTypeStrategy.count(entityClass);
    }

	public <T extends NodeBacked> Class<T> getJavaType(final Node node) {
		return nodeTypeStrategy.getJavaType(node);
	}

	public Node getReferenceNode() {
        return graphDatabaseService.getReferenceNode();
    }


    public Node getOrCreateSubReferenceNode(final RelationshipType relType) {
        return new GraphDatabaseUtil(graphDatabaseService).getOrCreateSubReferenceNode(relType);
    }

    public TransactionManager getTxManager() {
        return ((EmbeddedGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
    }

    public boolean transactionIsRunning() {
        try {
            return getTxManager().getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            log.error("Error accessing TransactionManager", e);
            return false;
        }
    }

    public void removeIndex(final Node node, final String propName) {
        getNodeIndex().remove(node, propName, null);
    }

    public void removeIndex(final String propName) {
        removeIndex(null,propName);
    }

    public void index(final Node node, final String propName, final Object newVal) {
        getNodeIndex().add(node, propName, newVal.toString());
    }

    public boolean canConvert(final Class<?> from, final Class<?> to) {
        return conversionService.canConvert(from,to);
    }

    public <T> T convert(final Object value, final Class<T> type) {
        return conversionService.convert(value,type);
    }

    public Iterable<? extends Node> getAllNodes() {
        return graphDatabaseService.getAllNodes();
    }

    public Transaction beginTx() {
        return graphDatabaseService.beginTx();
    }

    public Relationship getRelationshipById(final long id) {
        return graphDatabaseService.getRelationshipById(id);
    }
}

