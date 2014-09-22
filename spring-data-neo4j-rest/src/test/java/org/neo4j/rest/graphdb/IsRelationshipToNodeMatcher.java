/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.util.TestHelper;

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
        return TestHelper.firstRelationshipBetween( relationships, startNode, endNode ) != null;
    }

    public void describeTo( Description description ) {
        description.appendValue( startNode ).appendText( " to " ).appendValue( endNode ).appendText( "not contained in relationships" );
    }
}
