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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.StartNode;


import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipNodeFieldAccessorFactory implements FieldAccessorFactory {

	private Neo4jTemplate template;

    public RelationshipNodeFieldAccessorFactory(Neo4jTemplate template) {
		super();
		this.template = template;
	}

	@Override
    public boolean accept(final Neo4jPersistentProperty f) {
        return isStartNodeField(f) || isEndNodeField(f);
    }

    private boolean isEndNodeField(final Neo4jPersistentProperty f) {
        return f.isAnnotationPresent(EndNode.class);
    }

    private boolean isStartNodeField(final Neo4jPersistentProperty f) {
        return f.isAnnotationPresent(StartNode.class);
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        if (isStartNodeField(property)) {
            return new RelationshipNodeFieldAccessor(property, template) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getStartNode();
                }
            };

        }
        if (isEndNodeField(property)) {
            return new RelationshipNodeFieldAccessor(property, template) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getEndNode();
                }
            };
        }
        return null;
    }

    public static abstract class RelationshipNodeFieldAccessor implements FieldAccessor {

        private final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;

        public RelationshipNodeFieldAccessor(final Neo4jPersistentProperty property, final Neo4jTemplate template) {
            this.property = property;
            this.template = template;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            throw new InvalidDataAccessApiUsageException("Cannot change start or end node of existing relationship.");
        }

        @Override
        public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            final Relationship relationship = template.getPersistentState(entity);
            final Node node = getNode(relationship);
            if (node == null) {
                return null;
            }
            final Object result = template.createEntityFromState(node, (Class<?>) property.getType(), mappingPolicy);
            return doReturn(result);
        }

        protected abstract Node getNode(Relationship relationship);

        @Override
        public boolean isWriteable(final Object entity) {
            return false;
        }
        
		@Override
		public Object getDefaultValue() {
			return null;
		}
    }

}
