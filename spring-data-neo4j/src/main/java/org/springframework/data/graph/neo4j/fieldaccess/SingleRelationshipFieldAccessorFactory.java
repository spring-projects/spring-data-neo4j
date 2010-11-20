/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.doReturn;

public class SingleRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {
	@Override
	public boolean accept(final Field f) {
	    return NodeBacked.class.isAssignableFrom(f.getType());
	}

	@Override
	public FieldAccessor<NodeBacked, ?> forField(final Field field) {
	    final RelatedTo relAnnotation = getRelationshipAnnotation(field);
	    if (relAnnotation == null)
	        return new SingleRelationshipFieldAccessor(typeFrom(field), Direction.OUTGOING, targetFrom(field), graphDatabaseContext);
	    return new SingleRelationshipFieldAccessor(typeFrom(relAnnotation), dirFrom(relAnnotation), targetFrom(field), graphDatabaseContext);
	}

	public static class SingleRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor<NodeBacked> {
	    public SingleRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> clazz, final GraphDatabaseContext graphDatabaseContext) {
	        super(clazz, graphDatabaseContext, direction, type);
	    }

		@Override
	    public Object setValue(final NodeBacked entity, final Object newVal) {
	        final Node node=checkUnderlyingNode(entity);
	        if (newVal == null) {
	            removeMissingRelationships(node, Collections.<Node>emptySet());
	            return null;
	        }
	        final Set<Node> target=checkTargetIsSetOfNodebacked(Collections.singleton(newVal));
	        checkNoCircularReference(node,target);
	        removeMissingRelationships(node, target);
			createAddedRelationships(node,target);
	        return newVal;
		}

	    @Override
		public Object getValue(final NodeBacked entity) {
	        checkUnderlyingNode(entity);
	        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
            final NodeBacked singleEntity = result.isEmpty() ? null : result.iterator().next();
            return doReturn(singleEntity);
		}

	}
}
