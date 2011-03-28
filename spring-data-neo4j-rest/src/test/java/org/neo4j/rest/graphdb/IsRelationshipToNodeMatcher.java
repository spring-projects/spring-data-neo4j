package org.neo4j.rest.graphdb;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * @author mh
 * @since 24.01.11
 */
class IsRelationshipToNodeMatcher extends TypeSafeMatcher<Iterable<Relationship>> {
    private final Node startNode;
    private final Node endNode;

    public IsRelationshipToNodeMatcher( Node startNode, Node endNode ) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    @Override
    public boolean matchesSafely( Iterable<Relationship> relationships ) {
        return relationshipFromTo( relationships, startNode, endNode ) != null;
    }

    public static Relationship relationshipFromTo( Iterable<Relationship> relationships, final Node startNode, final Node endNode ) {
        for ( Relationship relationship : relationships ) {
            if ( relationship.getOtherNode( startNode ).equals( endNode ) ) return relationship;
        }
        return null;
    }

    public void describeTo( Description description ) {
        description.appendValue( startNode ).appendText( " to " ).appendValue( endNode ).appendText( "not contained in relationships" );
    }
}
