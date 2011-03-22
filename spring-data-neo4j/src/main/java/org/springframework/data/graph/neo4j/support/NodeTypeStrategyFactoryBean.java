package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.persistence.support.EntityInstantiator;

public class NodeTypeStrategyFactoryBean implements FactoryBean<NodeTypeStrategy> {
    private GraphDatabaseService graphDatabaseService;
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    public NodeTypeStrategyFactoryBean(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphEntityInstantiator = graphEntityInstantiator;
    }

    @Override
    public NodeTypeStrategy getObject() throws Exception {
        return new SubReferenceNodeTypeStrategy(graphDatabaseService, graphEntityInstantiator);
    }

    @Override
    public Class<?> getObjectType() {
        return SubReferenceNodeTypeStrategy.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
