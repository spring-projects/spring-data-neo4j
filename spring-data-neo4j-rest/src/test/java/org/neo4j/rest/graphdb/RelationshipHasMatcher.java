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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class RelationshipHasMatcher extends TypeSafeMatcher<Iterable<Relationship>>{
	
	private final Node node;
	private final Direction direction;
	private final List<String> typeNames;

	public RelationshipHasMatcher(Node startNode, Direction direction, RelationshipType... types){
		this.node = startNode;
		this.direction = direction;		
		this.typeNames = fillTypeNames(Arrays.asList(types));
	}
	
	
	
	@Override
	public void describeTo(Description description) {
		description.appendText("Not all relationships matched the constraints. Node: ").appendValue(node).appendText(" direction: ").appendValue(direction).appendText(" relationship type(s): ").appendValue(typeNames);
		
	}

	@Override
	public boolean matchesSafely(Iterable<Relationship> relationships) {
		for (Relationship relationship : relationships) {
			
			boolean isStartnode = this.node.equals(relationship.getStartNode());
			boolean isEndnode = this.node.equals(relationship.getEndNode());
			if (!isStartnode && !isEndnode){
				return false;
			}
			
			Direction relationshipDirection = isStartnode ? Direction.OUTGOING : Direction.INCOMING;
			
			if (this.direction != null && this.direction!= relationshipDirection){
				return false;
			}
						
						
			if(!this.typeNames.isEmpty() && !this.typeNames.contains(relationship.getType().name())){				
				return false;
			}
		}
		
		return true;
	}
	
	public static RelationshipHasMatcher match(Node startNode, Direction direction, RelationshipType... types){
		return new RelationshipHasMatcher(startNode, direction, types);
	}
	
	public static List<String> fillTypeNames(List<RelationshipType> types){
		List<String> names = new ArrayList<String>();
		for (RelationshipType type : types) {
			names.add(type.name());
		}
		return names;
	}

}
