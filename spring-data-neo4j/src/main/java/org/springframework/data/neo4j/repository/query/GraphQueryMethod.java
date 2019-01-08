/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
