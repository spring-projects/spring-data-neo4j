/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.EntityFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.ClassUtils;

/**
 * Convert OGM special {@link QueryResult} annotated types into SD understandable type.
 *
 * @author Nicolas Mervaillie
 */
class CustomResultConverter implements Converter<Object, Object> {

	private static final boolean HAS_ENTITY_INSTANTIATOR_FEATURE = ClassUtils.isPresent(
			"org.neo4j.ogm.metadata.reflect.ReflectionEntityInstantiator", CustomResultConverter.class.getClassLoader());
	private final MetaData metaData;
	private final Class returnedType;

	CustomResultConverter(MetaData metaData, Class<?> returnedType) {

		this.metaData = metaData;
		this.returnedType = returnedType;
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

		SingleUseEntityMapper mapper;
		if (HAS_ENTITY_INSTANTIATOR_FEATURE) {
			OgmReflectionEntityInstantiator entityInstantiator = new OgmReflectionEntityInstantiator(metaData);
			Optional<Constructor<?>> optionalConstructor = ReflectionUtils.findConstructor(SingleUseEntityMapper.class,
					metaData, entityInstantiator);
			// the constructor must exist
			Constructor<?> constructor = optionalConstructor.get();
			mapper = (SingleUseEntityMapper) BeanUtils.instantiateClass(constructor, metaData, entityInstantiator);
		} else {
			mapper = new SingleUseEntityMapper(metaData, new EntityFactory(metaData));
		}
		return mapper.map(returnedType, (Map<String, Object>) source);
	}
}
