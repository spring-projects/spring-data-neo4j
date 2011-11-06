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

package org.springframework.data.neo4j.support.relationship;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.node.EntityStateFactory;

public class RelationshipEntityStateFactory implements EntityStateFactory<Relationship> {

	private Neo4jTemplate template;
	
    private DelegatingFieldAccessorFactory relationshipDelegatingFieldAccessorFactory;
    private Neo4jMappingContext mappingContext;

    @SuppressWarnings("unchecked")
    public EntityState<Relationship> getEntityState(final Object entity, boolean detachable) {
        final Class<?> entityType = entity.getClass();
        final Neo4jPersistentEntity persistentEntity = (Neo4jPersistentEntity) mappingContext.getPersistentEntity(entityType);
        final RelationshipEntityState relationshipEntityState = new RelationshipEntityState(null, entity, entityType, template, relationshipDelegatingFieldAccessorFactory, persistentEntity);
        if (!detachable) {
            return relationshipEntityState;
        }
        return new DetachedEntityState<Relationship>(relationshipEntityState, template);

	}

	public void setTemplate(Neo4jTemplate template) {
		this.template = template;
	}

	public void setRelationshipDelegatingFieldAccessorFactory(
			DelegatingFieldAccessorFactory delegatingFieldAccessorFactory) {
		this.relationshipDelegatingFieldAccessorFactory = delegatingFieldAccessorFactory;
	}

    public void setMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public Neo4jTemplate getTemplate() {
        return template;
    }
}
