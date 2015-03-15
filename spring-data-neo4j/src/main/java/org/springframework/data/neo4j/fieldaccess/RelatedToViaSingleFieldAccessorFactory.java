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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class RelatedToViaSingleFieldAccessorFactory implements FieldAccessorFactory {

	private Neo4jTemplate template;
	
	public RelatedToViaSingleFieldAccessorFactory(Neo4jTemplate template) {
		this.template = template;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
        if (!property.isRelationship()) return false;
        return property.getRelationshipInfo().isRelatedToVia() && property.getRelationshipInfo().isSingle();
    }

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
		return new RelatedToViaSingleFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), template,property);
	}

	public static class RelatedToViaSingleFieldAccessor implements FieldAccessor {

        private final Class<?> relatedType;
        private final Neo4jTemplate template;
        private final Neo4jPersistentProperty property;
        private final RelationshipHelper relationshipHelper;
        private final RelationshipEntities relationshipEntities;

        public RelatedToViaSingleFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> relatedType, final Neo4jTemplate template, Neo4jPersistentProperty property) {
            relationshipHelper = new RelationshipHelper(template, direction, type);
            relationshipEntities = new RelationshipEntities(relationshipHelper, property);
            this.relatedType = relatedType;
            this.template = template;
            this.property = property;

        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            final Node startNode = relationshipHelper.checkAndGetNode(entity);
            final Map<Node,Object> endNodeToEntityMapping = relationshipEntities.loadEndNodeToRelationshipEntityMapping(startNode, toSet(newVal), relatedType);
            relationshipHelper.removeMissingRelationshipsInStoreAndKeepOnlyNewRelationShipsInSet(startNode, endNodeToEntityMapping.keySet(), null);
            persistEntities(endNodeToEntityMapping.values(), relationshipHelper.getRelationshipType() );
            return newVal;
	    }

        private Iterable<Object> toSet(Object newVal) {
            if (newVal==null) return Collections.emptySet();
            return Collections.singleton(newVal);
        }

        private void persistEntities( final Collection<Object> relationshipEntities, RelationshipType relationshipType ) {
            for (Object entity : relationshipEntities) {
                template.save(entity, relationshipType, template.getMappingPolicy(entity));
            }
        }

	    @Override
	    public boolean isWriteable(Object entity) {
	        return true;
	    }

	    @Override
	    public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            final Node node = relationshipHelper.checkAndGetNode(entity);
            Relationship rel = relationshipHelper.getSingleRelationship(node);
            return doReturn(rel==null ? null :  template.createEntityFromState(rel,relatedType,mappingPolicy)); // template.load(rel,relatedType));
        }
    }
}
