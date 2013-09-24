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

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.GraphDatabaseGlobalOperations;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.ResourceIterableClosableIterable;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

/**
 * Provides a Node Type Representation Strategy which makes use of Labels, and specifically
 * uses the Core API as the mechanism for dealing with this. (This is inline with how
 * the original Node Type Representation Strategies used to work - moving forward a
 * Cypher based one will be used, however this exists for comparison purposes at this
 * point in time in the development)
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class CoreAPIBasedLabelingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    // option A : store type in entity
    // public static final String TYPE_PROPERTY_NAME = "__type__";
    // option B : add special type label
    public static final String TYPE_LABEL_PREFIX = "__TYPE__";
    protected GraphDatabase graphDb;
    protected final Class<Node> clazz;

    public CoreAPIBasedLabelingNodeTypeRepresentationStrategy(GraphDatabase graphDb) {
        this.graphDb = graphDb;
        this.clazz = Node.class;
    }

    @Override
    public void writeTypeTo(Node state, StoredEntityType type) {
        if (type == null || !type.isNodeEntity()) return;

        ResourceIterable<Label> labels = state.getLabels();
        if (labels.iterator().hasNext()) {
            return; // already there
        }

        addLabel(state,type,true);
        for (StoredEntityType superType : type.getSuperTypes()) {
            addLabel(state, superType, false);
        }
    }

    private void addLabel(Node state, StoredEntityType type, boolean isPrimary) {
        String alias = type.getAlias().toString();
        state.addLabel(DynamicLabel.label(alias));
        if (isPrimary) {
            // option A : store type in entity
            // state.setProperty(TYPE_PROPERTY_NAME,alias);

            // option B : add special type label
            state.addLabel(DynamicLabel.label(TYPE_LABEL_PREFIX + alias));
        }
    }

    @Override
    public <U> ClosableIterable<Node> findAll(StoredEntityType type) {
        long count = 0;
        GraphDatabaseGlobalOperations globalOps = graphDb.getGlobalGraphOperations();
        final ResourceIterable<Node> rin = globalOps.getAllNodesWithLabel(DynamicLabel.label(type.getAlias().toString()));
        return new ResourceIterableClosableIterable(rin);
    }

    @Override
    public long count(StoredEntityType type) {
        long count = 0;
        GraphDatabaseGlobalOperations globalOps = graphDb.getGlobalGraphOperations();
        for (Node n : globalOps.getAllNodesWithLabel(DynamicLabel.label(type.getAlias().toString()))) {
            count++;
        }
        return count;
    }

    @Override
    public Object readAliasFrom(Node state) {
        if (state == null)
            throw new IllegalArgumentException("Node is null");

        // Option A: Derive alias from property
        // return state.getProperty(TYPE_PROPERTY_NAME);

        // Option B: Derive alias from special Label
        for (Label label: state.getLabels()) {
            if (label.name().startsWith(TYPE_LABEL_PREFIX)) {
                return label.name().substring(TYPE_LABEL_PREFIX.length());
            }
        }
        throw new IllegalStateException("No primary SDN label exists .. (i.e one with SDN_) ");
    }

    @Override
    public void preEntityRemoval(Node state) {
        // don't think we need to do anything here!
    }
}