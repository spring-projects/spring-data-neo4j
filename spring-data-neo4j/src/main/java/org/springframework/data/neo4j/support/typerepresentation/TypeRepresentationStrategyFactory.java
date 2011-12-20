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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;

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
        this.graphDatabaseService = graphDatabaseService;
        this.strategy = strategy;
    }
    
    public TypeRepresentationStrategyFactory(GraphDatabase graphDatabaseService,Strategy strategy, IndexProvider indexProvider) {
        this.indexProvider = indexProvider;
        this.graphDatabaseService = graphDatabaseService;
        this.strategy = strategy;
    }

    private static Strategy chooseStrategy(GraphDatabase graphDatabaseService) {
        if (isAlreadyIndexed(graphDatabaseService)) return Strategy.Indexed;
        if (isAlreadySubRef(graphDatabaseService)) return Strategy.SubRef;
        return Strategy.Indexed;
    }

    private static boolean isAlreadyIndexed(GraphDatabase graphDatabaseService) {
        try {
            final Index<PropertyContainer> index = graphDatabaseService.getIndex(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
            return index!=null && Node.class.isAssignableFrom(index.getEntityType());
        } catch(NoSuchIndexException nsie) {
            return false;
        }
    }

    private static boolean isAlreadySubRef(GraphDatabase graphDatabaseService) {
        for (Relationship rel : graphDatabaseService.getReferenceNode().getRelationships()) {
            if (rel.getType().name().startsWith(SubReferenceNodeTypeRepresentationStrategy.SUBREF_PREFIX)) {
                return true;
            }
        }
        return false;
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

    private enum Strategy {
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
        Indexed {
            @Override
            public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new IndexingNodeTypeRepresentationStrategy(graphDatabaseService, indexProvider);
            }

            @Override
            public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy(GraphDatabase graphDatabaseService, IndexProvider indexProvider) {
                return new IndexingRelationshipTypeRepresentationStrategy(graphDatabaseService, indexProvider);
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
