package org.springframework.datastore.graph.neo4j.support;

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
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.NodeTypeStrategy;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.persistence.support.AbstractMixinFields;
import org.springframework.persistence.support.EntityInstantiator;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * @author Michael Hunger
 * @since 13.09.2010
 */
@Configurable
public class GraphDatabaseContext implements NodeTypeStrategy {
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


    public <T extends NodeBacked> T createEntityFromState(final Node node, final Class<T> type) {
        return graphEntityInstantiator.createEntityFromState(node, type);
    }

    public <S, T> T createEntityFromState(final S state, final Class<T> type) {
        if (state instanceof Node)
            return (T) graphEntityInstantiator.createEntityFromState((Node) state, (Class<? extends NodeBacked>) type);
        else
            return (T) relationshipEntityInstantiator.createEntityFromState((Relationship) state, (Class<? extends RelationshipBacked>) type);

    }

    public <T extends RelationshipBacked> T createEntityFromState(final Relationship relationship, final Class<T> type) {
        return relationshipEntityInstantiator.createEntityFromState(relationship, type);
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

    @Override
    public void postEntityCreation(final NodeBacked entity) {
        nodeTypeStrategy.postEntityCreation(entity);
    }

    @Override
    public <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz) {
        return nodeTypeStrategy.findAll(clazz);
    }

    @Override
    public long count(final Class<? extends NodeBacked> entityClass) {
        return nodeTypeStrategy.count(entityClass);
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

    public void removeIndex(Node node, String propName) {
        indexService.removeIndex(node,propName);
    }

    public void index(Node node, String propName, Object newVal) {
        indexService.index(node,propName,newVal);
    }

    public boolean canConvert(Class<?> from, Class<?> to) {
        return conversionService.canConvert(from,to);
    }

    public <T> T convert(Object value, Class<T> type) {
        return conversionService.convert(value,type);
    }

    public Iterable<? extends Node> getAllNodes() {
        return graphDatabaseService.getAllNodes();
    }

    public Transaction beginTx() {
        return graphDatabaseService.beginTx();
    }
}

