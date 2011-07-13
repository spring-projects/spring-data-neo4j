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
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class OneToNRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {
	
	public OneToNRelationshipFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
		super(graphDatabaseContext);
	}

	@Override
	public boolean accept(final Field f) {
	    return Collection.class.isAssignableFrom(f.getType()) && hasValidRelationshipAnnotation(f);
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Field field) {
	    final RelatedTo relAnnotation = getRelationshipAnnotation(field);
	    return new OneToNRelationshipFieldAccessor(typeFrom(field, relAnnotation), dirFrom(relAnnotation), targetFrom(field, relAnnotation), graphDatabaseContext,field);
	}

	public static class OneToNRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor<NodeBacked> {

	    public OneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final GraphDatabaseContext graphDatabaseContext, Field field) {
	        super(elementClass, graphDatabaseContext, direction, type,field);
	    }

	    public Object setValue(final NodeBacked entity, final Object newVal) {
	        final Node node = checkUnderlyingNode(entity);
	        if (newVal == null) {
	            removeMissingRelationships(node, Collections.<Node>emptySet());
	            return null;
	        }
	        final Set<Node> targetNodes = checkTargetIsSetOfNodebacked(newVal);
	        removeMissingRelationships(node, targetNodes);
	        createAddedRelationships(node, targetNodes);
	        return createManagedSet(entity, (Set<NodeBacked>) newVal);
	    }

	    @Override
	    public Object getValue(final NodeBacked entity) {
	        checkUnderlyingNode(entity);
	        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
	        return doReturn(createManagedSet(entity, result));
	    }

	}
}
