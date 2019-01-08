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

import static java.lang.reflect.Proxy.*;

import java.util.Map;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * Convert OGM special {@link QueryResult} annotated types into SD understandable type.
 *
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class CustomResultConverter implements Converter<Object, Object> {

	private final MetaData metaData;
	private final Class returnedType;

	private final QueryResultInstantiator entityInstantiator;

	CustomResultConverter(MetaData metaData, Class<?> returnedType, QueryResultInstantiator entityInstantiator) {

		this.metaData = metaData;
		this.returnedType = returnedType;
		this.entityInstantiator = entityInstantiator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object convert(Object source) {

		if (returnedType.getAnnotation(QueryResult.class) == null) {
			return source;
		}
		if (returnedType.isInterface()) {
			Class<?>[] interfaces = new Class<?>[] { returnedType };
			return newProxyInstance(returnedType.getClassLoader(), interfaces,
					new QueryResultProxy((Map<String, Object>) source));
		}

		SingleUseEntityMapper mapper = new SingleUseEntityMapper(metaData, entityInstantiator);
		return mapper.map(returnedType, (Map<String, Object>) source);
	}
}
