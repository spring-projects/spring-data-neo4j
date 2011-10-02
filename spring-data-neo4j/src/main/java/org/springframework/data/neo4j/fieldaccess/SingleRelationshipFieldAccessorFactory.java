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

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.util.Collections;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class SingleRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {

	public SingleRelationshipFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
		super(graphDatabaseContext);
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
	    return property.isRelationship() && property.getRelationshipInfo().targetsNodes() && !property.getRelationshipInfo().isMultiple();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
        return new SingleRelationshipFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), graphDatabaseContext,property);
	}

	public static class SingleRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor {
	    public SingleRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> clazz, final GraphDatabaseContext graphDatabaseContext, Neo4jPersistentProperty property) {
	        super(clazz, graphDatabaseContext, direction, type, property);
	    }

		@Override
	    public Object setValue(final Object entity, final Object newVal) {
	        final Node node=checkUnderlyingNode(entity);
	        if (newVal == null) {
	            removeMissingRelationships(node, Collections.<Node>emptySet());
	            return null;
	        }
	        final Set<Node> target=checkTargetIsSetOfNodebacked(Collections.singleton(newVal));
	        removeMissingRelationships(node, target);
			createAddedRelationships(node,target);
	        return newVal;
		}

	    @Override
		public Object getValue(final Object entity) {
	        checkUnderlyingNode(entity);
	        final Set<Object> result = createEntitySetFromRelationshipEndNodes(entity);
            final Object singleEntity = result.isEmpty() ? null : result.iterator().next();
            return doReturn(singleEntity);
		}

	}
}
