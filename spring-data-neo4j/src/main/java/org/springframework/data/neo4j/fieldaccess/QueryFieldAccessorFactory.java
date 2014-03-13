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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.Query;

import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.util.TypeInformation;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class QueryFieldAccessorFactory implements FieldAccessorFactory {
    private final Neo4jTemplate template;

    public QueryFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return property.hasQuery();
    }


    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new QueryFieldAccessor(property, template);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class QueryFieldAccessor implements FieldAccessor {
	    protected final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;
        private final String query;
	    private Class<?> target;
        protected String[] annotationParams;
        private boolean iterableResult;
        private final CypherQueryEngine queryEngine;

        public QueryFieldAccessor(final Neo4jPersistentProperty property, Neo4jTemplate template) {
	        this.property = property;
            this.template = template;
            final Query query = property.findAnnotation(Query.class);
            this.annotationParams = query.params();
            if ((this.annotationParams.length % 2) != 0) {
                throw new IllegalArgumentException("Number of parameters has to be even to construct a parameter map");
            }
            this.query = property.getQuery();
            this.iterableResult = Iterable.class.isAssignableFrom(property.getType());
            this.target = resolveTarget(query,property);
            queryEngine = this.template.queryEngineFor();
        }

        private Class<?> resolveTarget(Query query, Neo4jPersistentProperty property) {
            if (!query.elementClass().equals(Object.class)) return query.elementClass();
            return property.getTypeInformation().getActualType().getType();
        }

        @Override
	    public boolean isWriteable(Object entity) {
	        return false;
	    }

	    @Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly query field " + property);
	    }

	    @Override
	    public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            return doReturn(executeQuery(entity, this.query, createPlaceholderParams(entity),mappingPolicy));
	    }

        private Object executeQuery(Object entity, String queryString, Map<String, Object> params, MappingPolicy mappingPolicy) {
            final TypeInformation<?> typeInformation = property.getTypeInformation();
            final TypeInformation<?> actualType = typeInformation.getActualType();
            final Class<?> targetType = actualType.getType();
            final Result<Map<String, Object>> result = queryEngine.query(queryString, params).with(mappingPolicy);
            if (actualType.isMap()) {
                return result;
            }
            if (typeInformation.isCollectionLike()) {
               return result.to(targetType);
            }
            return result.to(targetType).singleOrNull();
        }

        private Map<String, Object> createPlaceholderParams(Object entity) {
            Map<String,Object> params=new HashMap<String, Object>();
            final Node startNode = template.<Node>getPersistentState(entity);
            params.put("self", startNode.getId());
            if (annotationParams.length==0) return params;
            for (int i = 0; i < annotationParams.length; i+=2) {
                params.put(annotationParams[i],annotationParams[i+1]);
            }
            return params;
        }

		@Override
		public Object getDefaultValue() {
			return null;
		}
    }
}
