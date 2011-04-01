package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.*;
import org.springframework.data.persistence.EntityInstantiator;

public class TypeRepresentationStrategyFactory {
    private GraphDatabaseService graphDatabaseService;
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
    private EntityInstantiator<RelationshipBacked,Relationship> relationshipEntityInstantiator;
    private Strategy strategy;

    public TypeRepresentationStrategyFactory(GraphDatabaseService graphDatabaseService,
                                             EntityInstantiator<NodeBacked, Node> graphEntityInstantiator,
                                             EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphEntityInstantiator = graphEntityInstantiator;
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
        strategy = chooseStrategy();
    }

    private Strategy chooseStrategy() {
        if (isAlreadyIndexed()) return Strategy.Indexed;
        if (isAlreadySubRef()) return Strategy.SubRef;
        return Strategy.Indexed;
    }

    private boolean isAlreadyIndexed() {
        return graphDatabaseService.index().existsForNodes(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
    }

    private boolean isAlreadySubRef() {
        for (Relationship rel : graphDatabaseService.getReferenceNode().getRelationships()) {
            if (rel.getType().name().startsWith(SubReferenceNodeTypeRepresentationStrategy.SUBREF_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy() {
        return strategy.getNodeTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
    }

    public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy() {
        return strategy.getRelationshipTypeRepresentationStrategy(graphDatabaseService, relationshipEntityInstantiator);
    }

    private enum Strategy {
        SubRef {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new SubReferenceNodeTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
                return new NoopRelationshipTypeRepresentationStrategy();
            }
        },
        Indexed {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new IndexingNodeTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
                return new IndexingRelationshipTypeRepresentationStrategy(graphDatabaseService, relationshipEntityInstantiator);
            }
        },
        Noop {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
                return new NoopNodeTypeRepresentationStrategy();
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
                return new NoopRelationshipTypeRepresentationStrategy();
            }
        };

        public abstract NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator);

        public abstract RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator);
    }
}
