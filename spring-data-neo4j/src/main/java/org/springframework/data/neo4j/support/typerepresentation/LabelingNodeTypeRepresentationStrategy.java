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
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.repository.query.CypherQuery;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.mapping.WrappedIterableClosableIterable;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a Node Type Representation Strategy which makes use of Labels, and specifically
 * uses Cypher as the mechanism for interacting with the graph database.
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class LabelingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final Label SDN_LABEL_STRATEGY = DynamicLabel.label("SDN_LABEL_STRATEGY");
    public static final String LABELSTRATEGY_PREFIX = "__TYPE__";
    public static final long   REFERENCE_NODE_ID = 0L;

    protected GraphDatabase graphDb;
    protected final Class<Node> clazz;
    protected QueryEngine<CypherQuery> queryEngine;
    private boolean sdnLabelStrategyPresent;

    public LabelingNodeTypeRepresentationStrategy(GraphDatabase graphDb) {
        this.graphDb = graphDb;
        this.clazz = Node.class;
        this.queryEngine = graphDb.queryEngineFor(QueryType.Cypher);
        this.sdnLabelStrategyPresent = false;
    }

    @Override
    public void writeTypeTo(Node state, StoredEntityType type) {
        if (type == null || !type.isNodeEntity()) return;

        Label sdnLabel = DynamicLabel.label(LABELSTRATEGY_PREFIX + type.getAlias());
        if (state.hasLabel(sdnLabel)) {
            return; // already there
        }

        markSDNLabelStrategyInUseIfNotExists();
        addLabelsForEntityHierarchy(state,type);

    }

    /**
     * For each level in the entity hierarchy, this method will assign a
     * label (the label name is based on the alias associated with the
     * entity type at each level). Additionally, a special label is added
     * as the primary SDN marker Label.
     */
    private void addLabelsForEntityHierarchy(Node state, StoredEntityType type) {
        String addLabelStatement = String.format("start n=node({nodeId}) set n:`%s`:`%s`" , LABELSTRATEGY_PREFIX + type.getAlias(),type.getAlias());
        for (StoredEntityType superType : type.getSuperTypes()) {
            addLabelStatement += String.format(":`%s`", superType.getAlias());
        }
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("nodeId",state.getId());
        queryEngine.query( addLabelStatement, params);
    }

    /**
     * Checks if a special label (SDN_LABEL_STRATEGY) exists against the reference node, and
     * if it does not, it is added. This label serves as an indicator that the Labeling strategy
     * has/is being used on this data set.
     */
    private void markSDNLabelStrategyInUseIfNotExists() {
        if (!sdnLabelStrategyPresent) {
            String query = String.format("start n=node(%d) match n:`%s` return count(*) ", REFERENCE_NODE_ID, SDN_LABEL_STRATEGY.name());
            Long labelCount = queryEngine.query(query, Collections.EMPTY_MAP).to(Long.class).single();

            if (labelCount == 0) {
                String update = String.format("start n=node(%d) set n:`%s` ", REFERENCE_NODE_ID, SDN_LABEL_STRATEGY.name());
                queryEngine.query(update, Collections.EMPTY_MAP);
            }
            sdnLabelStrategyPresent = true;
        }
    }

    @Override
    public <U> ClosableIterable<Node> findAll(StoredEntityType type) {
        String query = String.format("match n:`%s` return n", type.getAlias().toString());
        Iterable<Node> rin = queryEngine.query(query, Collections.EMPTY_MAP).to(Node.class);
        return new WrappedIterableClosableIterable<Node>(rin);

    }

    @Override
    public long count(StoredEntityType type) {
        String query = String.format("match n:`%s` return count(*)", type.getAlias().toString());
        return queryEngine.query(query, Collections.EMPTY_MAP).to(Long.class).single();
    }

    @Override
    public Object readAliasFrom(Node state) {
        if (state == null)
            throw new IllegalArgumentException("Node is null");

        String query = String.format("start n=node(%d) return labels(n) as labels", state.getId());
        Map queryResult = queryEngine.query(query, Collections.EMPTY_MAP).to(Map.class).single();
        Iterable<String> labels = (Iterable)queryResult.get("labels");

        for (String label: labels) {
            if (label.startsWith(LABELSTRATEGY_PREFIX)) {
                return label.substring(LABELSTRATEGY_PREFIX.length());
            }
        }
        throw new IllegalStateException("No primary SDN label exists .. (i.e one with starting with " + LABELSTRATEGY_PREFIX + ") ");

    }

    @Override
    public void preEntityRemoval(Node state) {
        // don't think we need to do anything here!
    }

}