package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.persistence.EntityInstantiator;

public class NodeTypeStrategyFactoryBean implements FactoryBean<NodeTypeStrategy> {
    private GraphDatabaseService graphDatabaseService;
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
    private Strategy strategy;

    public NodeTypeStrategyFactoryBean(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphEntityInstantiator = graphEntityInstantiator;
        strategy = chooseStrategy();
    }

    private Strategy chooseStrategy() {
        if (isAlreadyIndexed()) return Strategy.Indexed;
        if (isAlreadySubRef()) return Strategy.SubRef;
        return Strategy.Indexed;
    }

    private boolean isAlreadyIndexed() {
        return graphDatabaseService.index().existsForNodes(IndexingNodeTypeStrategy.NODE_INDEX_NAME);
    }

    private boolean isAlreadySubRef() {
        for (Relationship rel : graphDatabaseService.getReferenceNode().getRelationships()) {
            if (rel.getType().name().startsWith(SubReferenceNodeTypeStrategy.SUBREF_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NodeTypeStrategy getObject() throws Exception {
        return strategy.getObject(graphDatabaseService, graphEntityInstantiator);
    }

    @Override
    public Class<?> getObjectType() {
        return strategy.getObjectType();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    private enum Strategy {
        SubRef {
            @Override
            NodeTypeStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new SubReferenceNodeTypeStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            Class<? extends NodeTypeStrategy> getObjectType() {
                return SubReferenceNodeTypeStrategy.class;
            }
        },
        Indexed {
            @Override
            NodeTypeStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new IndexingNodeTypeStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            Class<? extends NodeTypeStrategy> getObjectType() {
                return IndexingNodeTypeStrategy.class;
            }
        },
        Noop {
            @Override
            NodeTypeStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new NoopNodeTypeStrategy();
            }

            @Override
            Class<? extends NodeTypeStrategy> getObjectType() {
                return NoopNodeTypeStrategy.class;
            }
        };
        abstract NodeTypeStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator);
        abstract Class<? extends NodeTypeStrategy> getObjectType();
    }
}
