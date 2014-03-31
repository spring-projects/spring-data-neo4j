package org.springframework.data.neo4j.support.schema;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;

import java.util.Collection;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 20.01.14
 */
public class SchemaIndexProvider {
    private final GraphDatabase gd;
    private final CypherQueryEngine cypher;

    private static final Logger logger = LoggerFactory.getLogger(SchemaIndexProvider.class);

    public SchemaIndexProvider(GraphDatabase gd) {
        this.gd = gd;
        cypher = gd.queryEngine();
    }

    public void createIndex(Neo4jPersistentProperty property) {
        String label = getLabel(property);
        String prop = getName(property);
        boolean unique = property.getIndexInfo().isUnique();
        createIndex(label, prop, unique);
    }

    public void createIndex(String label, String prop, boolean unique) {
        String query = createIndexQuery(label, prop, unique);
        if (logger.isDebugEnabled()) logger.debug(query);
        cypher.query(query, null);
    }

    private String getName(Neo4jPersistentProperty property) {
        return property.getNeo4jPropertyName();
    }

    private String getLabel(Neo4jPersistentProperty property) {
        return property.getIndexInfo().getIndexName();
    }

    public <T> Result<T> findAll(Neo4jPersistentEntity entity) {
        String label = entity.getTypeAlias().toString();
        String query = findByLabelQuery(label);
        return cypher.query(query, null).<T>to(entity.getType());
    }

    public <T> Result<T> findByIndexedValue(Neo4jPersistentProperty property, Object value) {
        Result<Node> results = findAllNodes(property, value);
        return results.<T>to((Class<T>) property.getOwner().getType());
    }

    private Result<Node> findAllNodes(Neo4jPersistentProperty property, Object value) {
        IndexInfo indexInfo = property.getIndexInfo();
        String label = indexInfo.getIndexName();
        String prop = getName(property);
        String query = findByLabelAndPropertyQuery(label, prop);
        return cypher.query(query, map("value", value)).to(Node.class);
    }

    private String findByLabelQuery(String label) {
        return "MATCH (n:`"+label+"`) RETURN n";
    }

    public Node merge(String labelName, String key, Object value, final Map<String, Object> nodeProperties, Collection<String> labels) {
        if (labelName ==null || key == null || value==null) throw new IllegalArgumentException("Label "+ labelName +" key "+key+" and value must not be null");
        Map props = nodeProperties.containsKey(key) ? nodeProperties : MapUtil.copyAndPut(nodeProperties, key, value);
        Map<String, Object> params = map("props", props, "value", value);
        return cypher.query(mergeQuery(labelName, key,labels), params).to(Node.class).single();
    }

    private String mergeQuery(String labelName, String key, Collection<String> labels) {
        StringBuilder setLabels = new StringBuilder();
        if (labels!=null) {
            for (String label : labels) {
                if (label.equals(labelName)) continue;
                setLabels.append("SET n:").append(label).append(" ");
            }
        }
        return "MERGE (n:`"+labelName+"` {`"+key+"`: {value}}) ON CREATE SET n={props} "+setLabels+" return n";
    }


    private String findByLabelAndPropertyQuery(String label, String prop) {
        return "MATCH (n:`"+label+"` {`"+prop+"`:{value}}) RETURN n";
    }

    private String createIndexQuery(String label, String prop, boolean unique) {
        if (unique) {
            return  "CREATE CONSTRAINT ON (n:`"+ label +"`) ASSERT n.`"+ prop +"` IS UNIQUE";
        }
        return "CREATE INDEX ON :`"+ label +"`(`"+ prop +"`)";
    }

}
