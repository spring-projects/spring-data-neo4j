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
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.mapping.WrappedIterableClosableIterable;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides a Node Type Representation Strategy which makes use of a single Label per type.
 */
public class SingleLabelBasedNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final String SDN_LABEL_STRATEGY = "SDN_LABEL_STRATEGY";

    protected GraphDatabase graphDb;
    protected final Class<Node> clazz;
    protected final LabelBasedStrategyCypherHelper cypherHelper;
    protected CypherQueryEngine queryEngine;

    public SingleLabelBasedNodeTypeRepresentationStrategy(GraphDatabase graphDb) {
        this.graphDb = graphDb;
        this.clazz = Node.class;
        this.queryEngine = graphDb.queryEngine();
        this.cypherHelper = new LabelBasedStrategyCypherHelper(queryEngine);
        markSDNLabelStrategyInUse();
    }

    @Override
    public void writeTypeTo(Node state, StoredEntityType type) {
        if (type == null || !type.isNodeEntity()) return;

        Label sdnLabel = DynamicLabel.label(type.getAlias().toString());
        if (state.hasLabel(sdnLabel)) {
            return; // already there
        }
        addLabelsForEntityHierarchy(state, type);
    }

    /**
     * For each level in the entity hierarchy, this method will assign a
     * label (the label name is based on the alias associated with the
     * entity type at each level). Additionally, a special label is added
     * as the primary SDN marker Label.
     */
    private void addLabelsForEntityHierarchy(Node state, StoredEntityType type) {
        cypherHelper.setLabelsOnNode(state.getId(), getAllHierarchyLabelsForType(type));
    }

    private Set<String> getAllHierarchyLabelsForType(StoredEntityType type) {
        return Collections.singleton(type.getAlias().toString());
    }

    /**
     * Ensures that a special label (SDN_LABEL_STRATEGY) exists in the graph,
     * and if it does not, it is added. This label serves
     * as an indicator that the Labeling strategy has/is going to be used on this
     * data set.
     */
    private void markSDNLabelStrategyInUse() {
        cypherHelper.createMarkerLabel(SDN_LABEL_STRATEGY);
    }

    @Override
    public <U> ClosableIterable<Node> findAll(StoredEntityType type) {
        Iterable<Node> rin = cypherHelper.getNodesWithLabel(type.getAlias().toString());
        return new WrappedIterableClosableIterable(rin);
    }

    @Override
    public long count(StoredEntityType type) {
        return cypherHelper.countNodesWithLabel(type.getAlias().toString());
    }

    @Override
    public Object readAliasFrom(Node state) {
        if (state == null)
            throw new IllegalArgumentException("Node is null");

        Label label = IteratorUtil.firstOrNull(state.getLabels());
        String labelName = label != null ? label.name() : null;
        if (labelName != null) return labelName;
        throw new IllegalStateException("No primary SDN label exists .. (i.e one starting with " + ") ");
    }

    @Override
    public void preEntityRemoval(Node state) {
    }

    @Override
    public boolean isLabelBased() {
        return true;
    }

    public static boolean isStrategyAlreadyInUse(GraphDatabase graphDatabaseService) {
        return graphDatabaseService.getAllLabelNames().contains(SDN_LABEL_STRATEGY);
    }
}
