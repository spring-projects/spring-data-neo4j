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
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

/**
 * Provides a Node Type Representation Strategy which makes use of Labels, and specifically
 * uses Cypher as the mechanism for dealing with this.
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class CypherBasedLabelingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    // option A : store type in entity
    // public static final String TYPE_PROPERTY_NAME = "__type__";
    // option B : add special type label
    public static final String TYPE_LABEL_PREFIX = "__TYPE__";
    protected GraphDatabase graphDb;
    protected final Class<Node> clazz;

    public CypherBasedLabelingNodeTypeRepresentationStrategy(GraphDatabase graphDb) {
        this.graphDb = graphDb;
        this.clazz = Node.class;
    }

    @Override
    public void writeTypeTo(Node state, StoredEntityType type) {
        throw new RuntimeException("TODO");
    }

    private void addLabel(Node state, StoredEntityType type, boolean isPrimary) {
        throw new RuntimeException("TODO");
    }

    @Override
    public <U> ClosableIterable<Node> findAll(StoredEntityType type) {
        throw new RuntimeException("TODO");
    }

    @Override
    public long count(StoredEntityType type) {
        throw new RuntimeException("TODO");
    }

    @Override
    public Object readAliasFrom(Node state) {
        throw new RuntimeException("TODO");
    }

    @Override
    public void preEntityRemoval(Node state) {
        // don't think we need to do anything here!
    }
}