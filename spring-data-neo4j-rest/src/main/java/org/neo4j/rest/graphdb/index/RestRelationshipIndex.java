package org.neo4j.rest.graphdb.index;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.RestRelationship;
import org.neo4j.rest.graphdb.RestRequest;

import java.util.Map;

/**
 * @author mh
 * @since 24.01.11
 */
public class RestRelationshipIndex extends RestIndex<Relationship> implements RelationshipIndex {
    public RestRelationshipIndex( RestRequest restRequest, String indexName, RestGraphDatabase restGraphDatabase ) {
        super( restRequest, indexName, restGraphDatabase );
    }

    public Class<Relationship> getEntityType() {
        return Relationship.class;
    }

    public void remove(Relationship entity, String key) {
        throw new UnsupportedOperationException();
    }

    public void remove(Relationship entity) {
        throw new UnsupportedOperationException();
    }

    protected Relationship createEntity( Map<?, ?> item ) {
        return new RestRelationship( (Map<?, ?>) item, restGraphDatabase );
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> get( String s, Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> query( String s, Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> query( Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }
}
