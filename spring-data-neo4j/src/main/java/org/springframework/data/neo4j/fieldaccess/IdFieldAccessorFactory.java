/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.fieldaccess;


import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IdFieldAccessorFactory implements FieldAccessorFactory {
    private final Neo4jTemplate template;

    public IdFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
	public boolean accept(final Neo4jPersistentProperty property) {
	    return property.isIdProperty();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
	    return new IdFieldAccessor(property, template);
	}

	public static class IdFieldAccessor implements FieldAccessor {
	    protected final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;

        public IdFieldAccessor(final Neo4jPersistentProperty property, Neo4jTemplate template) {
	        this.property = property;
            this.template = template;
        }

	    @Override
	    public boolean isWriteable(Object entity) {
	        return false;
	    }

	    @Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
	        return newVal;
	    }

	    @Override
	    public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            final PropertyContainer state = template.getPersistentState(entity);
            if (state instanceof Node) {
                return doReturn(((Node)state).getId());
            }
            if (state instanceof Relationship) {
                return doReturn(((Relationship)state).getId());
            }
            throw new MappingException("Error retrieving id value from "+entity);
	    }

		@Override
		public Object getDefaultValue() {
			return null;
		}

	}
}
