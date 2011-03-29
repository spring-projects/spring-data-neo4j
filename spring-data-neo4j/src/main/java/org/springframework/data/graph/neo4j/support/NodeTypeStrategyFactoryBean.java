package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.TypeRepresentationStrategy;
import org.springframework.data.persistence.EntityInstantiator;

public class NodeTypeStrategyFactoryBean implements FactoryBean<TypeRepresentationStrategy> {
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
        return graphDatabaseService.index().existsForNodes(IndexingTypeRepresentationStrategy.NODE_INDEX_NAME);
    }

    private boolean isAlreadySubRef() {
        for (Relationship rel : graphDatabaseService.getReferenceNode().getRelationships()) {
            if (rel.getType().name().startsWith(SubReferenceTypeRepresentationStrategy.SUBREF_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TypeRepresentationStrategy getObject() throws Exception {
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
            TypeRepresentationStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new SubReferenceTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            Class<? extends TypeRepresentationStrategy> getObjectType() {
                return SubReferenceTypeRepresentationStrategy.class;
            }
        },
        Indexed {
            @Override
            TypeRepresentationStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new IndexingTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            Class<? extends TypeRepresentationStrategy> getObjectType() {
                return IndexingTypeRepresentationStrategy.class;
            }
        },
        Noop {
            @Override
            TypeRepresentationStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new NoopTypeRepresentationStrategy();
            }

            @Override
            Class<? extends TypeRepresentationStrategy> getObjectType() {
                return NoopTypeRepresentationStrategy.class;
            }
        };
        abstract TypeRepresentationStrategy getObject(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator);
        abstract Class<? extends TypeRepresentationStrategy> getObjectType();
    }
}
