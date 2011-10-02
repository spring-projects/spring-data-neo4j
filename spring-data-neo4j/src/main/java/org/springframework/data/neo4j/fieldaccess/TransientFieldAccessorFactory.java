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

import org.neo4j.graphdb.PropertyContainer;

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

public class TransientFieldAccessorFactory implements FieldAccessorFactory {
    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return property.isTransient();
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new TransientFieldAccessor(property);
    }

    /**
     * @author Michael Hunger
     * @since 12.09.2010
     */
    public static class TransientFieldAccessor implements FieldAccessor {
        protected final Neo4jPersistentProperty property;

        public TransientFieldAccessor(final Neo4jPersistentProperty property) {
            this.property = property;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal) {
            return newVal;
        }

        @Override
        public boolean isWriteable(final Object entity) {
            return true;
        }

        @Override
        public Object getValue(final Object entity) {
            return null;
        }

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

    }
}
