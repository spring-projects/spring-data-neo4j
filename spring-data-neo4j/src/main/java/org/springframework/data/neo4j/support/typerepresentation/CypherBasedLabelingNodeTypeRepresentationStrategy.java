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

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
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
public class CypherBasedLabelingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final String TYPE_PROPERTY_NAME = "__type__";
    protected GraphDatabase graphDb;
    protected final Class<Node> clazz;
    protected QueryEngine<CypherQuery> queryEngine;

    public CypherBasedLabelingNodeTypeRepresentationStrategy(GraphDatabase graphDb) {
        this.graphDb = graphDb;
        this.clazz = Node.class;
        this.queryEngine = graphDb.queryEngineFor(QueryType.Cypher);
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

        /*
            CYPHER DOES NOT LIKE THIS ... though it would be nice :)
            String addLabelStatement = "start n=node({nodeId}) set n:{alias}";
         */
        String addLabelStatement = buildQuery("start n=node({nodeId}) set n:" , alias);
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("nodeId",state.getId());
        //params.put("alias",alias);

        queryEngine.query( addLabelStatement, params);

        if (isPrimary) {
            String setPropertyStatement =
                    buildQuery("start n=node(", String.valueOf(state.getId()),")",
                               " set n.", TYPE_PROPERTY_NAME ,"='",alias,"'");
            queryEngine.query( setPropertyStatement, Collections.EMPTY_MAP);

            /*
            CYPHER DOES NOT LIKE THIS ...
            String setPropertyStatement = "start n=node({node_id}) set n.{property_name}={property_value}";
            params = new HashMap<String,Object>();
            params.put("node_id",state.getId());
            params.put("property_name",TYPE_PROPERTY_NAME);
            params.put("property_value",alias);
            queryEngine.query( setPropertyStatement, params);
            */
        }
    }

    @Override
    public <U> ClosableIterable<Node> findAll(StoredEntityType type) {
        String query = buildQuery( "match n:`" , type.getAlias().toString() , "` return n");
        Iterable<Node> rin = queryEngine.query(query, Collections.EMPTY_MAP).to(Node.class);
        return new WrappedIterableClosableIterable<Node>(rin);

    }

    @Override
    public long count(StoredEntityType type) {
        String query = buildQuery("match n:`" , type.getAlias().toString() , "` return count(*)");
        return queryEngine.query(query, Collections.EMPTY_MAP).to(Long.class).single();
    }

    @Override
    public Object readAliasFrom(Node state) {
        if (state == null)
            throw new IllegalArgumentException("Node is null");
        return state.getProperty(TYPE_PROPERTY_NAME);
    }

    @Override
    public void preEntityRemoval(Node state) {
        // don't think we need to do anything here!
    }

    private String buildQuery(String... strings) {
        StringBuffer sb = new StringBuffer();
        for (String s:strings) {
            sb.append(s);
        }
        return sb.toString();
    }
}