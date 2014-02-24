package org.springframework.data.neo4j.support.schema;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.query.QueryEngine;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 20.01.14
 */
public class SchemaIndexProvider {
    private final GraphDatabase gd;
    private final QueryEngine<Object> cypher;

    private static final Logger logger = LoggerFactory.getLogger(SchemaIndexProvider.class);

    public SchemaIndexProvider(GraphDatabase gd) {
        this.gd = gd;
        cypher = gd.queryEngineFor(QueryType.Cypher);
    }

    public void createIndex(Neo4jPersistentProperty property) {
        String label = getLabel(property);
        String prop = getName(property);
        String query = indexQuery(label, prop, property.getIndexInfo().isUnique());
        if (logger.isDebugEnabled()) logger.debug(query);
        cypher.query(query, null);
    }

    private String getName(Neo4jPersistentProperty property) {
        return property.getNeo4jPropertyName();
    }

    private String getLabel(Neo4jPersistentProperty property) {
        return property.getIndexInfo().getIndexName();
    }

    public <T> EndResult<T> findAll(Neo4jPersistentEntity entity) {
        String label = entity.getTypeAlias().toString();
        String query = findByLabelQuery(label);
        return cypher.query(query, null).<T>to(entity.getType());
    }

    public <T> EndResult<T> findAll(Neo4jPersistentProperty property, Object value) {
        IndexInfo indexInfo = property.getIndexInfo();
        String label = indexInfo.getIndexName();
        String prop = getName(property);
        String query = findByLabelAndPropertyQuery(label, prop);
        return cypher.query(query, map("value", value)).<T>to((Class<T>)property.getOwner().getType());
    }

    private String findByLabelQuery(String label) {
        return "MATCH (n:`"+label+"`) RETURN n";
    }

    private String findByLabelAndPropertyQuery(String label, String prop) {
        return "MATCH (n:`"+label+"` {`"+prop+"`:{value}}) RETURN n";
    }

    private String indexQuery(String label, String prop, boolean unique) {
        if (unique) {
            return  "CREATE CONSTRAINT ON (n:`"+ label +"`) ASSERT n.`"+ prop +"` IS UNIQUE";
        }
        return "CREATE INDEX ON :`"+ label +"`(`"+ prop +"`)";
    }

    interface IndexCreator {
        void deferCreateIndex(Neo4jPersistentProperty property);
    }
}
