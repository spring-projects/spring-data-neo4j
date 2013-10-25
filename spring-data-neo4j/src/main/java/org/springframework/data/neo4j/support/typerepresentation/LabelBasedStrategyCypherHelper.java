package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.repository.query.CypherQuery;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    static final String CYPHER_ADD_LABELS_TO_NODE = "match (n) where id(n)={nodeId} set n%s";
    static final String CYPHER_COUNT_LABELS_ON_NODE = "match (n) where id(n)={nodeId} and n:`%s` return count(*) ";
    static final String CYPHER_RETURN_NODES_WITH_LABEL = "match (n:`%s`) return n";
    static final String CYPHER_RETURN_COUNT_OF_NODES_WITH_LABEL = "match (n:`%s`) return count(*)";
    static final String CYPHER_RETURN_LABELS_FOR_NODE = "match (n) where id(n)={nodeId} return labels(n) as labels";

    private QueryEngine<CypherQuery> queryEngine;

    public LabelBasedStrategyCypherHelper(QueryEngine<CypherQuery> queryEngine) {
        this.queryEngine = queryEngine;
    }

    public void setLabelOnNode(Long nodeId, String label) {
        String addLabelStatement = String.format(CYPHER_ADD_LABEL_TO_NODE , label);
        queryEngine.query( addLabelStatement, getParamsWithNodeId(nodeId) );
    }

    public void setLabelsOnNode(Long nodeId, String labelString) {
        String addLabelStatement = String.format(CYPHER_ADD_LABELS_TO_NODE , labelString);
        queryEngine.query( addLabelStatement, getParamsWithNodeId(nodeId) );
    }

    public boolean doesNodeHaveLabel(Long nodeId, String label) {
        String query = String.format(CYPHER_COUNT_LABELS_ON_NODE,  label);
        Result<CypherQuery> result =  queryEngine.query(query, getParamsWithNodeId(nodeId));
        long labelCount = result.to(Number.class).single().longValue();
        return labelCount > 0;
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
        Result<CypherQuery> result = queryEngine.query(query, Collections.EMPTY_MAP);
        return result.to(Number.class).single().longValue();
    }

    public Iterable<String> getLabelsForNode(long nodeId) {
        Map queryResult = queryEngine.query(CYPHER_RETURN_LABELS_FOR_NODE, getParamsWithNodeId(nodeId)).to(Map.class).single();
        return (Iterable)queryResult.get("labels");
    }
}
