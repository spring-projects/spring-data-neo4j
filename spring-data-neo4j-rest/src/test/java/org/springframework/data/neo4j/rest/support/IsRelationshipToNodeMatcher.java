/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.support;

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
