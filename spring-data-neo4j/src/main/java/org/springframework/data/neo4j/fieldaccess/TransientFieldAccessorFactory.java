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
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;

public class TransientFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {
    @Override
    public boolean accept(final Neo4JPersistentProperty property) {
        return property.isTransient();
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>> forField(final Neo4JPersistentProperty property) {
        return new TransientFieldAccessor(property);
    }

    /**
     * @author Michael Hunger
     * @since 12.09.2010
     */
    public static class TransientFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>> {
        protected final Neo4JPersistentProperty property;

        public TransientFieldAccessor(final Neo4JPersistentProperty property) {
            this.property = property;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            return newVal;
        }

        @Override
        public boolean isWriteable(final GraphBacked<PropertyContainer> graphBacked) {
            return true;
        }

        @Override
        public Object getValue(final GraphBacked<PropertyContainer> graphBacked) {
            return null;
        }

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

    }
}
