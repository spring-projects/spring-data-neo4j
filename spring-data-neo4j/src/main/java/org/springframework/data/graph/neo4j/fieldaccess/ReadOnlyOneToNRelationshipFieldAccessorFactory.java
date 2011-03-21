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
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

public class ReadOnlyOneToNRelationshipFieldAccessorFactory extends NodeRelationshipFieldAccessorFactory {

	public ReadOnlyOneToNRelationshipFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
		super(graphDatabaseContext);
	}

	@Override
	public boolean accept(final Field f) {
	    return Iterable.class.equals(f.getType()) && hasValidRelationshipAnnotation(f);
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Field field) {
	    final RelatedTo relAnnotation = getRelationshipAnnotation(field);
	    return new ReadOnlyOneToNRelationshipFieldAccessor(typeFrom(field, relAnnotation), dirFrom(relAnnotation), targetFrom(relAnnotation), graphDatabaseContext);
	}

	public static class ReadOnlyOneToNRelationshipFieldAccessor extends OneToNRelationshipFieldAccessorFactory.OneToNRelationshipFieldAccessor {

		public ReadOnlyOneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final GraphDatabaseContext graphDatabaseContext) {
	        super(type,direction,elementClass, graphDatabaseContext);
		}

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    public Object setValue(final NodeBacked entity, final Object newVal) {
			throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
		}

	}
}
