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

package org.springframework.data.neo4j.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.session.EntityInstantiator;
import org.neo4j.ogm.session.PropertyWriter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implements OGM instantiation callback in order to user Spring Data Commons infrastructure for instantiation.
 *
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
public class Neo4jOgmEntityInstantiatorAdapter implements EntityInstantiator {

	private final MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context;
	private ConversionService conversionService;
	private final EntityInstantiators instantiators;

	public Neo4jOgmEntityInstantiatorAdapter(MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context,
			@Nullable ConversionService conversionService) {
		Assert.notNull(context, "MappingContext cannot be null");

		this.context = context;
		this.conversionService = conversionService;
		instantiators = new EntityInstantiators();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <T> T createInstance(Class<T> clazz, Map<String, Object> propertyValues) {

		Neo4jPersistentEntity<T> persistentEntity = (Neo4jPersistentEntity<T>) context.getRequiredPersistentEntity(clazz);
		org.springframework.data.convert.EntityInstantiator instantiator = instantiators
				.getInstantiatorFor(persistentEntity);

		return instantiator.createInstance(persistentEntity, getParameterProvider(propertyValues, conversionService));
	}

	@Override
	public <T> boolean needsFurtherPopulation(Class<T> clazz, T instance) {

		// Class needs further population if not a persistent entity or
		// is a persistent entity that requires property population.
		return Optional.of(clazz)
				.map(context::getPersistentEntity)
				.map(Neo4jPersistentEntity::requiresPropertyPopulation)
				.orElse(true);
	}

	@Override
	public <T> Function<T, PropertyWriter<T>> getPropertyWriterSupplier() {

		return initialInstance -> {
			final Neo4jPersistentEntity persistentEntity = context.getPersistentEntity(initialInstance.getClass());
			final PropertyWriter fallback = EntityInstantiator.super.getPropertyWriterSupplier().apply(initialInstance);
			final PersistentPropertyAccessor<T> propertyAccessor = persistentEntity.getPropertyAccessor(initialInstance);

			return new PropertyWriter<T>() {
				@Override
				public T getInstance() {
					return propertyAccessor.getBean();
				}

				@Override
				public void writeTo(String propertyName, FieldInfo targetProperty, Object value) {
					String name = Optional.ofNullable(targetProperty).map(FieldInfo::getName).orElse(propertyName);
					PersistentProperty property = persistentEntity.getPersistentProperty(name);
					if (property == null) {
						fallback.writeTo(propertyName, targetProperty, value);
					} else {
						propertyAccessor.setProperty(property, value);
						propertyAccessor.getBean();
					}
				}
			};
		};
	}

	private ParameterValueProvider<Neo4jPersistentProperty> getParameterProvider(Map<String, Object> propertyValues,
			ConversionService conversionService) {
		return new Neo4jPropertyValueProvider(propertyValues, conversionService);
	}

	private static class Neo4jPropertyValueProvider implements ParameterValueProvider<Neo4jPersistentProperty> {

		private Map<String, Object> propertyValues;
		private ConversionService conversionService;

		Neo4jPropertyValueProvider(Map<String, Object> propertyValues, ConversionService conversionService) {

			Assert.notNull(propertyValues, "Properties cannot be null");

			this.propertyValues = propertyValues;
			this.conversionService = conversionService;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		@Nullable
		public Object getParameterValue(PreferredConstructor.Parameter parameter) {
			Object value = propertyValues.get(parameter.getName());
			if (value == null || conversionService == null) {
				return value;
			} else {
				return conversionService.convert(value, parameter.getType().getType());
			}
		}
	}
}
