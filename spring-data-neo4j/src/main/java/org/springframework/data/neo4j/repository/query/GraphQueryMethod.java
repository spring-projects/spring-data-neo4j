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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.support.GenericTypeExtractor;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
* @author mh
* @since 31.10.11
*/
public class GraphQueryMethod extends QueryMethod {

    private final Method method;
    private final NamedQueries namedQueries;
    private final Neo4jMappingContext mappingContext;
    private final Query queryAnnotation;

    public GraphQueryMethod(Method method, RepositoryMetadata metadata, NamedQueries namedQueries, Neo4jMappingContext mappingContext) {
        super(method, metadata);
        this.method = method;
        this.namedQueries = namedQueries;
        this.mappingContext = mappingContext;
        this.queryAnnotation = method.getAnnotation(Query.class);
    }

    public String getQueryString() {
        return queryAnnotation != null ? queryAnnotation.value() : getNamedQuery(getNamedQueryName());
    }

    @Override
    public String getNamedQueryName() {
		String annotatedName = queryAnnotation != null ? queryAnnotation.queryName() : null;
        return StringUtils.hasText(annotatedName) ? annotatedName : super.getNamedQueryName();
    }

    public String getCountQueryString() {
        return queryAnnotation != null ? queryAnnotation.countQuery() : getNamedQuery(getNamedCountQueryName());
    }

    public boolean isValid() {
        return this.getQueryString() != null; // && this.compoundType != null
    }

    private String getNamedQuery(String namedQueryName) {
        if (namedQueries.hasQuery(namedQueryName)) {
            return namedQueries.getQuery(namedQueryName);
        }
        return null;
    }

    String getNamedCountQueryName() {
		String annotatedName = queryAnnotation != null ? queryAnnotation.countQueryName() : null;
		return StringUtils.hasText(annotatedName) ? annotatedName : getNamedQueryName() + ".count";
	}

    
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    protected Map<String, Object> resolveParams(ParameterAccessor accessor, ParameterResolver parameterResolver) {
        Map<Parameter, Object> parameters = parameterResolver.resolveParameters(getParameterValues(accessor));

        return nameParameters(parameters);
    }

    private Map<Parameter, Object> getParameterValues(ParameterAccessor accessor) {
        Map<Parameter,Object> parameters=new LinkedHashMap<Parameter, Object>();
        for (Parameter parameter : getParameters().getBindableParameters()) {
            final Object value = accessor.getBindableValue(parameter.getIndex());
            parameters.put(parameter,value);
        }
        return parameters;
    }

    private Map<String, Object> nameParameters(Map<Parameter, Object> parameters) {
        Map<String, Object> params = new HashMap<String, Object>();
        for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
            params.put(getParameterName(entry.getKey()), entry.getValue());
        }
        return params;
    }

    private String getParameterName(Parameter parameter) {
        final String parameterName = parameter.getName();
        if (parameterName != null) {
            return parameterName;
        }
        return String.valueOf(parameter.getIndex());
    }

    Class<?> getCompoundType() {
        final Class<?> elementClass = getElementClass();
        if (elementClass!=null) {
            return elementClass;
        }
        return GenericTypeExtractor.resolveReturnedType(method);
    }

    private Class<?> getElementClass() {
        if (!hasAnnotation() || queryAnnotation.elementClass().equals(Object.class)) {
            return null;
        }
        return queryAnnotation.elementClass();
    }

    public boolean hasAnnotation() {
        return queryAnnotation!=null;
    }

    boolean isIterableResult() {
        return hasResultOfType(Iterable.class);
    }

    public RepositoryQuery createQuery(final Neo4jTemplate template) {
        if (queryAnnotation == null) {
            if (namedQueries.hasQuery(getNamedQueryName())) {
                return new CypherGraphRepositoryQuery(this, template); // cypher is default for named queries
            } else {
                return new DerivedCypherRepositoryQuery(mappingContext, this, template);
            }
        }
        return new CypherGraphRepositoryQuery(this, template);
    }

    public boolean isSetResult() {
        final Class<Set> superClass = Set.class;
        return hasResultOfType(superClass);
    }

    public boolean hasResultOfType(Class<?> superClass) {
        return superClass.isAssignableFrom(getReturnType());
    }

    public boolean isCollectionResult() {
        return hasResultOfType(Collection.class);
    }

    @Override
    public String toString() {
        return "Repository-Graph-Query-Method for "+method;
    }
}
