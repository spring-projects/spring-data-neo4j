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

import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Collections;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class RelatedToSingleFieldAccessorFactory implements FieldAccessorFactory {

    protected Neo4jTemplate template;

    public RelatedToSingleFieldAccessorFactory(Neo4jTemplate template) {
		this.template = template;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
	    if (!property.isRelationship()) return false;
        return property.getRelationshipInfo().isRelatedTo() && property.getRelationshipInfo().isSingle();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
        return new RelatedToSingleFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), template,property);
	}

	public static class RelatedToSingleFieldAccessor extends RelatedToFieldAccessor {
	    public RelatedToSingleFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> clazz, final Neo4jTemplate template, Neo4jPersistentProperty property) {
	        super(clazz, template, direction, type, property);
	    }

		@Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
	        final Node node= checkAndGetNode(entity);
	        if (newVal == null) {
	            removeMissingRelationships(node, Collections.<Node>emptySet());
	            return null;
	        }
	        final Set<Node> target= createSetOfTargetNodes(Collections.singleton(newVal));
	        removeMissingRelationships(node, target);
			createAddedRelationships(node,target);
	        return newVal;
		}

	    @Override
		public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
	        checkAndGetNode(entity);
	        final Set<Object> result = createEntitySetFromRelationshipEndNodes(entity, property.obtainMappingPolicy(mappingPolicy));
            final Object singleEntity = result.isEmpty() ? null : result.iterator().next();
            return doReturn(singleEntity);
		}

	}
}
