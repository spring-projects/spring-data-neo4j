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
package org.neo4j.rest.graphdb.traversal;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.PruneEvaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.Predicate;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public interface RestTraversalDescription extends TraversalDescription {
	RestTraversalDescription prune(ScriptLanguage language, String code);
	
	RestTraversalDescription prune(PruneEvaluator pruneEvaluator);

	RestTraversalDescription filter(ScriptLanguage language, String code);

	RestTraversalDescription maxDepth(int depth);
	
	RestTraversalDescription breadthFirst();
	
	RestTraversalDescription relationships(RelationshipType relationshipType, Direction direction);
	
	RestTraversalDescription filter(Predicate<Path> pathPredicate);	
	

    public enum ScriptLanguage {
        JAVASCRIPT;
    }
}
