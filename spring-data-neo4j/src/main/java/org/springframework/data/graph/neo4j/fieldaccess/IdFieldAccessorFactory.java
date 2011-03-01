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

import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.core.NodeBacked;

import javax.persistence.Id;
import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IdFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
	public boolean accept(final Field f) {
	    return isIdField(f);
	}

	private boolean isIdField(Field field) {
	    final Class<?> type = field.getType();
		return (type.equals(Long.class) || type.equals(long.class)) && (field.isAnnotationPresent(GraphId.class) || field.isAnnotationPresent(Id.class));
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Field field) {
	    return new IdFieldAccessor(field);
	}

	public static class IdFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Field field;

	    public IdFieldAccessor(final Field field) {
	        this.field = field;
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

	}
}
