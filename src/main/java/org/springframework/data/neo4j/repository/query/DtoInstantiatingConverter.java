/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.Assert;

/**
 * {@link Converter} to instantiate DTOs from fully equipped domain objects.
 * The original idea of this converter and it's usage is to be found in Spring Data Mongo. Thanks to the original
 * authors Oliver Drotbohm and Mark Paluch.
 *
 * @author Michael J. Simons
 * @soundtrack Gustavo Santaolalla - The Last Of Us
 */
class DtoInstantiatingConverter implements Converter<EntityInstanceWithSource, Object> {

	private final Class<?> targetType;
	private final Neo4jMappingContext context;

	/**
	 * Creates a new {@link Converter} to instantiate DTOs.
	 *
	 * @param dtoType must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 */
	DtoInstantiatingConverter(Class<?> dtoType, Neo4jMappingContext context) {

		Assert.notNull(dtoType, "DTO type must not be null!");
		Assert.notNull(context, "MappingContext must not be null!");

		this.targetType = dtoType;
		this.context = context;
	}

	@Override
	public Object convert(EntityInstanceWithSource entityInstanceAndSource) {

		Object entityInstance = entityInstanceAndSource.getEntityInstance();
		if (targetType.isInterface() || targetType.isInstance(entityInstance)) {
			return entityInstance;
		}

		PersistentEntity<?, ?> sourceEntity = context.getRequiredPersistentEntity(entityInstance.getClass());
		PersistentPropertyAccessor sourceAccessor = sourceEntity.getPropertyAccessor(entityInstance);
		PersistentEntity<?, ?> targetEntity = context.getRequiredPersistentEntity(targetType);
		PreferredConstructor<?, ? extends PersistentProperty<?>> constructor = targetEntity
				.getPersistenceConstructor();

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Object dto = context.getInstantiatorFor(targetEntity)
				.createInstance(targetEntity, new ParameterValueProvider() {
					@Override
					public Object getParameterValue(Parameter parameter) {
						PersistentProperty<?> targetProperty = targetEntity.getPersistentProperty(parameter.getName());
						if (targetProperty == null) {
							throw new MappingException("Cannot map constructor parameter " + parameter.getName()
									+ " to a property of class " + targetType);
						}
						return getPropertyValueFor(targetProperty, sourceEntity, sourceAccessor,
								entityInstanceAndSource);
					}
				});

		PersistentPropertyAccessor dtoAccessor = targetEntity.getPropertyAccessor(dto);
		targetEntity.doWithProperties((SimplePropertyHandler) property -> {

			if (constructor.isConstructorParameter(property)) {
				return;
			}

			Object propertyValue = getPropertyValueFor(property, sourceEntity, sourceAccessor, entityInstanceAndSource);
			dtoAccessor.setProperty(property, propertyValue);
		});

		return dto;
	}

	Object getPropertyValueFor(PersistentProperty<?> targetProperty, PersistentEntity<?, ?> sourceEntity,
			PersistentPropertyAccessor sourceAccessor, EntityInstanceWithSource entityInstanceAndSource) {

		TypeSystem typeSystem = entityInstanceAndSource.getTypeSystem();
		Record sourceRecord = entityInstanceAndSource.getSourceRecord();

		String targetPropertyName = targetProperty.getName();
		PersistentProperty<?> sourceProperty = sourceEntity.getPersistentProperty(targetPropertyName);
		if (sourceProperty != null) {
			return sourceAccessor.getProperty(sourceProperty);
		}

		if (!sourceRecord.containsKey(targetPropertyName)) {
			Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() -> String.format(""
							+ "Cannot retrieve a value for property `%s` of DTO `%s` and the property will always be null. "
							+ "Make sure to project only properties of the domain type of use a custom query that "
							+ "returns a mappable data under the name `%1$s`.",
					targetPropertyName, targetType.getName()));
		} else if (targetProperty.isMap()) {
			Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() -> String.format(""
					+ "%s is an additional property to be projected. "
					+ "However, map properties cannot be projected and the property will always be null."));
		} else {
			// We don't support associations on the top level of DTO projects which is somewhat inline with the restrictions
			// regarding DTO projections as described in https://docs.spring.io/spring-data/jpa/docs/2.4.0-RC1/reference/html/#projections.dtos
			// > except that no proxying happens and no nested projections can be applied
			// Therefore, we extract associations kinda half-manual.

			Value property = sourceRecord.get(targetPropertyName);
			if (targetProperty.isCollectionLike() && !typeSystem.LIST().isTypeOf(property)) {
				Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() -> String.format(""
						+ "%s is a list property but the selected value is not a list and the property will always be null."
				));
			} else {
				Class<?> actualType = targetProperty.getActualType();

				Function<Value, Object> singleValue;
				if (context.hasPersistentEntityFor(actualType)) {
					singleValue = p -> context.getEntityConverter().read(actualType, p);
				} else {
					ClassTypeInformation<?> actualTargetType = ClassTypeInformation.from(actualType);
					singleValue = p -> context.getConversionService().readValue(p, actualTargetType, null);
				}

				if (targetProperty.isCollectionLike()) {
					List<Object> returnedValues = property.asList(singleValue);
					Collection<Object> target = CollectionFactory
							.createCollection(targetProperty.getType(), actualType, returnedValues.size());
					target.addAll(returnedValues);
					return target;
				} else {
					return singleValue.apply(property);
				}
			}
		}

		return null;
	}
}
