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
import org.neo4j.index.IndexHits;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.persistence.support.EntityInstantiator;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * @author Michael Hunger
 * @since 13.09.2010
 */
@Configurable
public class GraphDatabaseContext {
    @Autowired
    private GraphDatabaseService graphDatabaseService;

    @Autowired
    public EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

/*    @Autowired
    private DelegatingFieldAccessorFactory fieldAccessorFactory;
*/
    @Autowired
    public EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

    @Autowired
    private IndexService indexService;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private NodeTypeStrategy nodeTypeStrategy;
    private final static Log log = LogFactory.getLog(GraphDatabaseContext.class);

    public Node createNode() {
        return graphDatabaseService.createNode();
    }

    public <S, T> T createEntityFromState(final S state, final Class<T> type) {
        if (state instanceof Node)
            return (T) graphEntityInstantiator.createEntityFromState((Node) state, getJavaType((Node) state));
        else
            return (T) relationshipEntityInstantiator.createEntityFromState((Relationship) state, (Class<? extends RelationshipBacked>) type);
    }

    public IndexHits<Node> getIndexedNodes(final String property, final Object value) {
        return indexService.getNodes(property, value);
    }

    public Node getSingleIndexedNode(final String property, final Object value) {
        return indexService.getSingleNode(property, value);
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
        indexService.removeIndex(node,propName);
    }

    public void removeIndex(final String propName) {
        indexService.removeIndex(propName);
    }

    public void index(final Node node, final String propName, final Object newVal) {
        indexService.index(node,propName,newVal);
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

