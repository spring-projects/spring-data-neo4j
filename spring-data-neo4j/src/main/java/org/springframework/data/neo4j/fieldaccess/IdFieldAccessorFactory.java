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

import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IdFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
	public boolean accept(final Neo4JPersistentProperty property) {
	    return property.isIdProperty();
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Neo4JPersistentProperty property) {
	    return new IdFieldAccessor(property);
	}

	public static class IdFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Neo4JPersistentProperty property;

	    public IdFieldAccessor(final Neo4JPersistentProperty property) {
	        this.property = property;
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        return newVal;
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
            return doReturn(nodeBacked.getPersistentState().getId());
	    }

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

	}
}
