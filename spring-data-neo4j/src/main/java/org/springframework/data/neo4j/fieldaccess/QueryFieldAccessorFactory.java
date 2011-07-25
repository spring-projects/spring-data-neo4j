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

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GenericTypeExtractor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class QueryFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
    public boolean accept(final Field f) {
        final Query query = f.getAnnotation(Query.class);
        return query != null
                && !query.value().isEmpty();
    }


    @Override
    public FieldAccessor<NodeBacked> forField(final Field field) {
        return new QueryFieldAccessor(field);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class QueryFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Field field;
	    private final String query;
	    private Class<?> target;
        protected String[] annotationParams;
        private boolean iterableResult;

        public QueryFieldAccessor(final Field field) {
	        this.field = field;
            final Query query = field.getAnnotation(Query.class);
            this.annotationParams = query.params();
            if ((this.annotationParams.length % 2) != 0) {
                throw new IllegalArgumentException("Number of parameters has to be even to construct a parameter map");
            }
            this.query = query.value();
            this.iterableResult = Iterable.class.isAssignableFrom(field.getType());
            this.target = resolveTarget(query,field);
        }

        private Class<?> resolveTarget(Query query, Field field) {
            if (!query.elementClass().equals(Object.class)) return query.elementClass();
            return GenericTypeExtractor.resolveFieldType(field);
        }

        @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly query field " + field);
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
            return doReturn(executeQuery(nodeBacked, this.query, createPlaceholderParams(nodeBacked)));
	    }

        private Object executeQuery(NodeBacked nodeBacked, String queryString, Map<String, Object> params) {
            if (!iterableResult) return nodeBacked.findByQuery(queryString,this.target,params);
            if (Map.class.isAssignableFrom(target)) return nodeBacked.findAllByQuery(queryString,params);
            return nodeBacked.findAllByQuery(queryString, this.target,params);
        }

        private Map<String, Object> createPlaceholderParams(NodeBacked nodeBacked) {
            Map<String,Object> params=new HashMap<String, Object>();
            params.put("start",nodeBacked.getNodeId());
            if (annotationParams.length==0) return params;
            for (int i = 0; i < annotationParams.length; i+=2) {
                params.put(annotationParams[i],annotationParams[i+1]);
            }
            return params;
        }
    }
}
