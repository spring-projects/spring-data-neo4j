/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link Converter} to instantiate DTOs from fully equipped domain objects. The original
 * idea of this converter and it's usage is to be found in Spring Data Mongo. Thanks to
 * the original authors Oliver Drotbohm and Mark Paluch.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "6.1.2")
public final class DtoInstantiatingConverter implements Converter<EntityInstanceWithSource, Object> {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(DtoInstantiatingConverter.class));

	private final Class<?> targetType;

	private final Neo4jMappingContext context;

	/**
	 * Creates a new {@link Converter} to instantiate DTOs.
	 * @param dtoType must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 */
	public DtoInstantiatingConverter(Class<?> dtoType, Neo4jMappingContext context) {

		Assert.notNull(dtoType, "DTO type must not be null");
		Assert.notNull(context, "MappingContext must not be null");

		this.targetType = dtoType;
		this.context = context;
	}

	public Object convertDirectly(Object entityInstance) {
		Neo4jPersistentEntity<?> sourceEntity = this.context.getRequiredPersistentEntity(entityInstance.getClass());
		PersistentPropertyAccessor<Object> sourceAccessor = sourceEntity.getPropertyAccessor(entityInstance);

		Neo4jPersistentEntity<?> targetEntity = this.context.addPersistentEntity(TypeInformation.of(this.targetType))
			.orElseThrow(() -> new IllegalStateException("Target entity could not be created for a DTO"));
		InstanceCreatorMetadata<?> creator = targetEntity.getInstanceCreatorMetadata();

		Object dto = this.context.getInstantiatorFor(targetEntity)
			.createInstance(targetEntity, getParameterValueProvider(targetEntity,
					targetProperty -> getPropertyValueDirectlyFor(targetProperty, sourceEntity, sourceAccessor)));

		PersistentPropertyAccessor<Object> dtoAccessor = targetEntity.getPropertyAccessor(dto);
		PropertyHandlerSupport.of(targetEntity).doWithProperties(property -> {

			if (creator != null && creator.isCreatorParameter(property)) {
				return;
			}

			Object propertyValue = getPropertyValueDirectlyFor(property, sourceEntity, sourceAccessor);
			dtoAccessor.setProperty(property, propertyValue);
		});

		return dto;
	}

	@Nullable Object getPropertyValueDirectlyFor(PersistentProperty<?> targetProperty, PersistentEntity<?, ?> sourceEntity,
			PersistentPropertyAccessor<?> sourceAccessor) {

		String targetPropertyName = targetProperty.getName();
		PersistentProperty<?> sourceProperty = sourceEntity.getPersistentProperty(targetPropertyName);

		if (sourceProperty == null) {
			return null;
		}

		Object result = sourceAccessor.getProperty(sourceProperty);
		if (result != null && targetProperty.isEntity()
				&& !targetProperty.getTypeInformation().isAssignableFrom(sourceProperty.getTypeInformation())) {
			return new DtoInstantiatingConverter(targetProperty.getType(), this.context).convertDirectly(result);
		}
		return result;
	}

	@Override
	@Nullable public Object convert(EntityInstanceWithSource entityInstanceAndSource) {

		Object entityInstance = entityInstanceAndSource.getEntityInstance();
		if (this.targetType.isInterface() || this.targetType.isInstance(entityInstance)) {
			return entityInstance;
		}

		Neo4jPersistentEntity<?> sourceEntity = this.context.getRequiredPersistentEntity(entityInstance.getClass());
		PersistentPropertyAccessor<Object> sourceAccessor = sourceEntity.getPropertyAccessor(entityInstance);

		Neo4jPersistentEntity<?> targetEntity = this.context.addPersistentEntity(TypeInformation.of(this.targetType))
			.orElseThrow(() -> new MappingException("Could not add a persistent entity for the projection target type '"
					+ this.targetType.getName() + "'"));
		InstanceCreatorMetadata<@NonNull ? extends PersistentProperty<?>> creator = targetEntity
			.getInstanceCreatorMetadata();

		Object dto = this.context.getInstantiatorFor(targetEntity)
			.createInstance(targetEntity,
					getParameterValueProvider(targetEntity, targetProperty -> getPropertyValueFor(targetProperty,
							sourceEntity, sourceAccessor, entityInstanceAndSource)));

		PersistentPropertyAccessor<Object> dtoAccessor = targetEntity.getPropertyAccessor(dto);
		targetEntity.doWithAll(property -> setPropertyOnDtoObject(entityInstanceAndSource, sourceEntity, sourceAccessor,
				creator, dtoAccessor, property));

		return dto;
	}

	private ParameterValueProvider<Neo4jPersistentProperty> getParameterValueProvider(
			Neo4jPersistentEntity<?> targetEntity, Function<Neo4jPersistentProperty, Object> extractFromSource) {
		return new ParameterValueProvider<>() {
			@SuppressWarnings("unchecked") // Needed for the last cast. It's easier that
											// way than using the parameter type info and
											// checking for primitives
			@Override
			public <T> T getParameterValue(Parameter<T, Neo4jPersistentProperty> parameter) {
				String parameterName = parameter.getName();
				if (parameterName == null) {
					throw new MappingException(
							"Constructor parameter names aren't available, please recompile your domain");
				}
				Neo4jPersistentProperty targetProperty = targetEntity.getPersistentProperty(parameterName);
				if (targetProperty == null) {
					throw new MappingException("Cannot map constructor parameter " + parameterName
							+ " to a property of class " + DtoInstantiatingConverter.this.targetType);
				}
				return (T) extractFromSource.apply(targetProperty);
			}
		};
	}

	private void setPropertyOnDtoObject(EntityInstanceWithSource entityInstanceAndSource,
			PersistentEntity<?, ?> sourceEntity, PersistentPropertyAccessor<Object> sourceAccessor,
			@Nullable InstanceCreatorMetadata<?> creator, PersistentPropertyAccessor<Object> dtoAccessor,
			Neo4jPersistentProperty property) {

		if (creator != null && creator.isCreatorParameter(property)) {
			return;
		}

		Object propertyValue = getPropertyValueFor(property, sourceEntity, sourceAccessor, entityInstanceAndSource);
		dtoAccessor.setProperty(property, propertyValue);
	}

	@Nullable Object getPropertyValueFor(Neo4jPersistentProperty targetProperty, PersistentEntity<?, ?> sourceEntity,
			PersistentPropertyAccessor<?> sourceAccessor, EntityInstanceWithSource entityInstanceAndSource) {

		TypeSystem typeSystem = entityInstanceAndSource.getTypeSystem();
		MapAccessor sourceRecord = entityInstanceAndSource.getSourceRecord();

		String targetPropertyName = targetProperty.getName();
		PersistentProperty<?> sourceProperty = sourceEntity.getPersistentProperty(targetPropertyName);
		if (sourceProperty != null) {
			return sourceAccessor.getProperty(sourceProperty);
		}

		if (!sourceRecord.containsKey(targetPropertyName)) {
			log.warn(() -> String.format(
					"" + "Cannot retrieve a value for property `%s` of DTO `%s` and the property will always be null. "
							+ "Make sure to project only properties of the domain type or use a custom query that "
							+ "returns a mappable data under the name `%1$s`.",
					targetPropertyName, this.targetType.getName()));
		}
		else if (targetProperty.isMap()) {
			log.warn(() -> String.format(
					"" + "%s is an additional property to be projected. "
							+ "However, map properties cannot be projected and the property will always be null.",
					targetPropertyName));
		}
		else {
			// We don't support associations on the top level of DTO projects which is
			// somewhat inline with the restrictions
			// regarding DTO projections as described in
			// https://docs.spring.io/spring-data/jpa/docs/2.4.0-RC1/reference/html/#projections.dtos
			// > except that no proxying happens and no nested projections can be applied
			// Therefore, we extract associations kinda half-manual.

			Value property = sourceRecord.get(targetPropertyName);
			if (targetProperty.isCollectionLike() && !typeSystem.LIST().isTypeOf(property)) {
				log.warn(() -> String.format(""
						+ "%s is a list property but the selected value is not a list and the property will always be null.",
						targetPropertyName));
			}
			else {
				Class<?> actualType = targetProperty.getActualType();

				Function<Value, Object> singleValue;
				if (this.context.hasPersistentEntityFor(actualType)) {
					singleValue = p -> this.context.getEntityConverter().read(actualType, p);
				}
				else {
					TypeInformation<?> actualTargetType = TypeInformation.of(actualType);
					singleValue = p -> this.context.getConversionService()
						.readValue(p, actualTargetType, targetProperty.getOptionalConverter());
				}

				if (targetProperty.isCollectionLike()) {
					List<Object> returnedValues = property.asList(singleValue);
					Collection<Object> target = CollectionFactory.createCollection(targetProperty.getType(), actualType,
							returnedValues.size());
					target.addAll(returnedValues);
					return target;
				}
				else {
					return singleValue.apply(property);
				}
			}
		}

		return null;
	}

}
