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

package org.springframework.data.graph.neo4j.support;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HasRelationshipMatcher extends TypeSafeMatcher<Node>
{
    private final String relationshipTypeName;
    private Node other;
    private Iterable<Relationship> relationships;

    HasRelationshipMatcher( String relationshipTypeName, Node other )
    {
        this.relationshipTypeName = relationshipTypeName;
        this.other = other;
    }

    @Override
    public boolean matchesSafely( Node item )
    {
        relationships = item.getRelationships();

        if (other==null) return getRelationships( item ).hasNext();

        for (Relationship relationship : relationships) {
            if (relationship.getOtherNode(item).equals(other)) {
                return true;
            }
        }
        return false;
    }

    public Iterator<Relationship> getRelationships( Node node )
    {
        return node.getRelationships( DynamicRelationshipType.withName( relationshipTypeName ) ).iterator();
    }


    @Override
    public void describeTo( Description description )
    {
        description.appendText( "Expected relationship named " + relationshipTypeName + " to " +(other==null ? "unspecified": other)+"\r\n    got: " );

        List<String> types = new ArrayList<String>();
        for ( Relationship rel : relationships )
        {
            types.add( describeRelationship( rel ) );
        }
        description.appendValueList( "[", ", ", "]", types );
    }

    private String describeRelationship( Relationship rel )
    {
        return rel.getType().name();
    }

    @Factory
    public static HasRelationshipMatcher hasRelationship( String typeName )
    {
        return new HasRelationshipMatcher( typeName, null );
    }
    @Factory
    public static HasRelationshipMatcher hasRelationship( String typeName , Node other)
    {
        return new HasRelationshipMatcher( typeName, other );
    }
    @Factory
    public static Matcher<Node> hasNoRelationship( String typeName , Node other)
    {
        return  CoreMatchers.not(new HasRelationshipMatcher( typeName, other ));
    }

}
