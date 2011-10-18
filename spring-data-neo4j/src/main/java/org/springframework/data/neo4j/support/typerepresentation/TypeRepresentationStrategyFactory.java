/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.*;
import org.springframework.data.neo4j.mapping.EntityInstantiator;

public class TypeRepresentationStrategyFactory {
    private GraphDatabaseService graphDatabaseService;
    private EntityInstantiator<Node> graphEntityInstantiator;
    private EntityInstantiator<Relationship> relationshipEntityInstantiator;
    private Strategy strategy;

    public TypeRepresentationStrategyFactory(GraphDatabaseService graphDatabaseService,
                                             EntityInstantiator<Node> graphEntityInstantiator,
                                             EntityInstantiator<Relationship> relationshipEntityInstantiator) {
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
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Node> graphEntityInstantiator) {
                return new SubReferenceNodeTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Relationship> relationshipEntityInstantiator) {
                return new NoopRelationshipTypeRepresentationStrategy(relationshipEntityInstantiator);
            }
        },
        Indexed {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Node> graphEntityInstantiator) {
                return new IndexingNodeTypeRepresentationStrategy(graphDatabaseService, graphEntityInstantiator);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Relationship> relationshipEntityInstantiator) {
                return new IndexingRelationshipTypeRepresentationStrategy(graphDatabaseService, relationshipEntityInstantiator);
            }
        },
        Noop {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Node> graphEntityInstantiator) {
                return new NoopNodeTypeRepresentationStrategy(graphEntityInstantiator);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Relationship> relationshipEntityInstantiator) {
                return new NoopRelationshipTypeRepresentationStrategy(relationshipEntityInstantiator);
            }
        };

        public abstract NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Node> graphEntityInstantiator);

        public abstract RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDatabaseService, EntityInstantiator<Relationship> relationshipEntityInstantiator);
    }
}
