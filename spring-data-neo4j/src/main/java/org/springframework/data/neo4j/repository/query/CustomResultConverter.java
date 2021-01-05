/*
 * Copyright 2011-2021 the original author or authors.
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

import static java.lang.reflect.Proxy.*;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.EntityFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.lang.Nullable;

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

	private final Optional<Constructor<?>> singleUseEntityMapperUsingInstantiator;
	private final Object entityInstantiator;

	CustomResultConverter(MetaData metaData, Class<?> returnedType,
			Optional<Constructor<?>> singleUseEntityMapperUsingInstantiator, @Nullable Object entityInstantiator) {

		this.metaData = metaData;
		this.returnedType = returnedType;

		this.singleUseEntityMapperUsingInstantiator = singleUseEntityMapperUsingInstantiator;
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

		SingleUseEntityMapper mapper = this.singleUseEntityMapperUsingInstantiator.map(
				constructor -> (SingleUseEntityMapper) BeanUtils.instantiateClass(constructor, metaData, entityInstantiator))
				.orElseGet(() -> new SingleUseEntityMapper(metaData, new EntityFactory(metaData)));
		return mapper.map(returnedType, (Map<String, Object>) source);
	}
}
