/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.repository.query.derived.DerivedGraphRepositoryQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 */
public class GraphQueryMethod extends QueryMethod {

	private final Session session;
	private final Method method;
	private final Query queryAnnotation;
	private final Integer queryDepthParamIndex;
	private final Integer queryDepth;
	private boolean staticDepth;

	public GraphQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory, Session session) {
		super(method, metadata, factory);
		this.method = method;
		this.session = session;
		this.queryAnnotation = method.getAnnotation(Query.class);
		this.queryDepthParamIndex = getQueryDepthParamIndex(method);
		this.queryDepth = getStaticQueryDepth(method);
		if (queryDepth != null && queryDepthParamIndex != null) {
			throw new IllegalArgumentException(method.getName() + " cannot have both a method @Depth and a parameter @Depth");
		}

	}

	public String getQuery() {
		return queryAnnotation.value();
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public String getNamedQueryName() {
		throw new UnsupportedOperationException("OGM does not currently support named queries.");
	}

	/**
	 * @return The concrete, non-generic return type of this query method - i.e., the type to which graph database query
	 *         results should be mapped
	 */
	public Class<?> resolveConcreteReturnType() {
		Class<?> type = this.method.getReturnType();
		Type genericType = this.method.getGenericReturnType();

		if (Iterable.class.isAssignableFrom(type)) {
			if (genericType instanceof ParameterizedType) {
				ParameterizedType returnType = (ParameterizedType) genericType;
				Type componentType = returnType.getActualTypeArguments()[0];

				if (componentType instanceof TypeVariable) {
					return this.getDomainClass();
				}
				return componentType instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) componentType).getRawType()
						: (Class<?>) componentType;
			} else {
				return Object.class;
			}
		}

		return type;
	}

	public RepositoryQuery createQuery() {
		if (method.getAnnotation(Query.class) != null) {
			if (resolveConcreteReturnType().isAnnotationPresent(QueryResult.class)) {
				return new QueryResultGraphRepositoryQuery(this, session);
			}
			return new GraphRepositoryQuery(this, session);
		}
		return new DerivedGraphRepositoryQuery(this, session);

	}

	public Integer getQueryDepthParamIndex() {
		return queryDepthParamIndex;
	}

	public Integer getQueryDepth() {
		return queryDepth;
	}

	public boolean hasStaticDepth() {
		return staticDepth;
	}

	private Integer getQueryDepthParamIndex(Method method) {
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].length > 0) {
				for (Annotation annotation : annotations[i]) {
					if (annotation.annotationType() == Depth.class) {
						if (method.getParameterTypes()[i] == Integer.class || method.getParameterTypes()[i] == int.class) {
							return i;
						} else {
							throw new IllegalArgumentException("Depth parameter in " + method.getName() + " must be an integer");
						}
					}
				}
			}
		}
		/*
		//Java 8 only
		Parameter[] parameters = method.getParameters();
		 for (int i = 0; i < method.getParameterCount(); i++) {
		     if (parameters[i].isAnnotationPresent(Depth.class)) {
		         if (parameters[i].getType() == Integer.class || parameters[i].getType() == int.class) {
		             return i;
		         }
		         else {
		             throw new IllegalArgumentException("Depth parameter in " + method.getName() + " must be an integer");
		         }
		     }
		 }*/
		return null;
	}

	private Integer getStaticQueryDepth(Method method) {
		if (method.isAnnotationPresent(Depth.class)) {
			staticDepth = true;
			return method.getAnnotation(Depth.class).value();
		}
		return null;
	}

	public String getCountQueryString() {
		return queryAnnotation != null ? queryAnnotation.countQuery() : null;
	}
}
