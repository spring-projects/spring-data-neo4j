package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.repository.query.CypherQuery;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.*;

import static org.neo4j.helpers.collection.Iterables.join;

/**
 * Provides some helper Cypher based functionality specifically
 * for use by the Label Based Node Type Representation Strategy.
 * This class may well become more broadly accessible / generic in time
 * however for now, serves to specifically aid the
 *  Label Based Node Type Representation Strategy.
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class LabelBasedStrategyCypherHelper {

    static final String CYPHER_ADD_LABEL_TO_NODE = "match (n) where id(n)={nodeId} set n:`%s`";
    static final String CYPHER_CREATE_MARKER_LABEL = "match (n) with n limit 1 set n:`%1$s` remove n:`%1$s` return count(*)";
    static final String CYPHER_ADD_LABELS_TO_NODE = "match (n) where id(n)={nodeId} set n%s";
    static final String CYPHER_COUNT_LABELS_ON_NODE = "match (n) where id(n)={nodeId} and n:`%s` return count(*) ";
    static final String CYPHER_RETURN_NODES_WITH_LABEL = "match (n:`%s`) return n";
    static final String CYPHER_RETURN_COUNT_OF_NODES_WITH_LABEL = "match (n:`%s`) return count(*)";
    static final String CYPHER_RETURN_LABELS_FOR_NODE = "match (n) where id(n)={nodeId} return labels(n) as labels";

    private CypherQueryEngine queryEngine;

    public LabelBasedStrategyCypherHelper(CypherQueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    public void setLabelOnNode(Long nodeId, String label) {
        String addLabelStatement = String.format(CYPHER_ADD_LABEL_TO_NODE , label);
        queryEngine.query( addLabelStatement, getParamsWithNodeId(nodeId) );
    }

    public void createMarkerLabel(String label) {
        String addLabelStatement = String.format(CYPHER_CREATE_MARKER_LABEL , label);
        queryEngine.query( addLabelStatement, null );
    }

    public void setLabelsOnNode(Long nodeId, Collection<String> labelString) {
        String addLabelStatement = formatAddLabelString(labelString);
        queryEngine.query( addLabelStatement, getParamsWithNodeId(nodeId) );
    }

    private String formatAddLabelString(Collection<String> labels) {
        return String.format(CYPHER_ADD_LABELS_TO_NODE,":`"+join("`:`",labels)+"`");
    }

    public Iterable<Node> getNodesWithLabel(String label) {
        String query = String.format(CYPHER_RETURN_NODES_WITH_LABEL, label);
        return queryEngine.query(query, Collections.EMPTY_MAP).to(Node.class);
    }



    public String buildLabelString(String... labels) {
        String result = "";
        for (String label: labels) {
            result += ":`" + label + "`";
        }
        return result;
    }

    private Map<String, Object> getParamsWithNodeId(long id) {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("nodeId",id);
        return params;
    }

    public long countNodesWithLabel(String label) {
        String query = String.format(CYPHER_RETURN_COUNT_OF_NODES_WITH_LABEL,  label);
        Result<Map<String, Object>> result = queryEngine.query(query, Collections.EMPTY_MAP);
        return result.to(Number.class).single().longValue();
    }

    public Iterable<String> getLabelsForNode(long nodeId) {
        Map queryResult = queryEngine.query(CYPHER_RETURN_LABELS_FOR_NODE, getParamsWithNodeId(nodeId)).to(Map.class).single();
        return (Iterable)queryResult.get("labels");
    }
}
