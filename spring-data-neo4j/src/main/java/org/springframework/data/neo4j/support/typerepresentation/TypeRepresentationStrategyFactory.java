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

import org.neo4j.graphdb.*;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;

public class TypeRepresentationStrategyFactory {
    private final GraphDatabase graphDatabaseService;
    private final Strategy strategy;
    private IndexProvider indexProvider;

    public TypeRepresentationStrategyFactory(GraphDatabase graphDatabaseService) {
        this(graphDatabaseService,chooseStrategy(graphDatabaseService), null);
    }
    
    public TypeRepresentationStrategyFactory(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
        this(graphDatabaseService,chooseStrategy(graphDatabaseService), indexProvider);
    }
    
    public TypeRepresentationStrategyFactory(GraphDatabase graphDatabaseService,Strategy strategy) {
        this(graphDatabaseService, strategy, null);
    }

    public TypeRepresentationStrategyFactory(GraphDatabase graphDatabaseService,Strategy strategy,
                                             IndexProvider indexProvider) {
        this.indexProvider = indexProvider;
        this.graphDatabaseService = graphDatabaseService;
        this.strategy = strategy;
    }

    private static Strategy chooseStrategy(GraphDatabase graphDatabaseService) {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            if (AbstractIndexBasedTypeRepresentationStrategy.isStrategyAlreadyInUse(graphDatabaseService)) return Strategy.Indexed;
            if (SubReferenceNodeTypeRepresentationStrategy.isStrategyAlreadyInUse(graphDatabaseService)) return Strategy.SubRef;
            if (LabelBasedNodeTypeRepresentationStrategy.isStrategyAlreadyInUse(graphDatabaseService)) return Strategy.Labeled;
            tx.success();
            return Strategy.Labeled;
        }
    }

    public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy() {
        return strategy.getNodeTypeRepresentationStrategy(graphDatabaseService, indexProvider);
    }

    public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy() {
        return strategy.getRelationshipTypeRepresentationStrategy(graphDatabaseService, indexProvider);
    }
    
    public void setIndexProvider(IndexProvider indexProvider) {
        this.indexProvider = indexProvider;
    }

    public enum Strategy {
        SubRef {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new SubReferenceNodeTypeRepresentationStrategy(graphDatabaseService);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new NoopRelationshipTypeRepresentationStrategy();
            }
        },
        Labeled {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new LabelBasedNodeTypeRepresentationStrategy(graphDatabaseService);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new IndexBasedRelationshipTypeRepresentationStrategy(graphDatabaseService, indexProvider);
            }
        },
        Indexed {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new IndexBasedNodeTypeRepresentationStrategy(graphDatabaseService, indexProvider);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new IndexBasedRelationshipTypeRepresentationStrategy(graphDatabaseService, indexProvider);
            }
        },
        Noop {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new NoopNodeTypeRepresentationStrategy();
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new NoopRelationshipTypeRepresentationStrategy();
            }
        };

        public abstract NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider);

        public abstract RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider);
    }
}
