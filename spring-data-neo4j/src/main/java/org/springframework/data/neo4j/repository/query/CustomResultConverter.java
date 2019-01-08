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
import static org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension.*;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.EntityFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

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
	private final MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

	private final Optional<Constructor<?>> singleUseEntityMapperUsingInstantiator;
	private final Object entityInstantiator;

	CustomResultConverter(MetaData metaData, Class<?> returnedType,
			@Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

		this.metaData = metaData;
		this.returnedType = returnedType;
		this.mappingContext = mappingContext;

		if (HAS_ENTITY_INSTANTIATOR_FEATURE) {
			this.entityInstantiator = getEntityInstantiator(metaData, mappingContext);
			this.singleUseEntityMapperUsingInstantiator = ReflectionUtils.findConstructor(SingleUseEntityMapper.class,
					metaData, entityInstantiator);
		} else {
			this.entityInstantiator = null;
			this.singleUseEntityMapperUsingInstantiator = Optional.empty();
		}
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

	private static Object getEntityInstantiator(MetaData metaData,
			@Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

		try {
			Class<?> queryResultInstantiatorClass = Class.forName(
					"org.springframework.data.neo4j.repository.query.QueryResultInstantiator", true,
					ClassUtils.getDefaultClassLoader());
			Optional<Constructor<?>> queryResultInstantiatorConstructor = ReflectionUtils
					.findConstructor(queryResultInstantiatorClass, metaData, mappingContext);
			return queryResultInstantiatorConstructor.map(ctor -> BeanUtils.instantiateClass(ctor, metaData, mappingContext))
					.get();
		} catch (ClassNotFoundException e) {
			// It is there, we just don't want to reference it directly.
			return null;
		}
	}
}
