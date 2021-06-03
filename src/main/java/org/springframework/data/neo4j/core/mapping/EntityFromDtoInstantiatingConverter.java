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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Converter} to instantiate entity objects from DTOs
 *
 */
@API(status = API.Status.INTERNAL, since = "6.1.2")
public final class EntityFromDtoInstantiatingConverter<T> implements Converter<Object, T> {

	private final Class<?> targetEntityType;
	private final Neo4jMappingContext context;

	/**
	 * Creates a new {@link Converter} to instantiate Entities from DTOs.
	 *
	 * @param entityType must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 */
	public EntityFromDtoInstantiatingConverter(Class<T> entityType, Neo4jMappingContext context) {

		Assert.notNull(entityType, "Entity type must not be null!");
		Assert.notNull(context, "MappingContext must not be null!");

		this.targetEntityType = entityType;
		this.context = context;
	}

	@Override
	public T convert(Object dtoInstance) {

		if (dtoInstance == null) {
			return null;
		}

		PersistentEntity<?, ?> sourceEntity = context.addPersistentEntity(ClassTypeInformation.from(dtoInstance.getClass())).get();
		PersistentPropertyAccessor<Object> sourceAccessor = sourceEntity.getPropertyAccessor(dtoInstance);

		PersistentEntity<?, ?> targetEntity = context.getPersistentEntity(targetEntityType);
		PreferredConstructor<?, ? extends PersistentProperty<?>> constructor = targetEntity
				.getPersistenceConstructor();

		@SuppressWarnings({ "rawtypes", "unchecked" })
		T entity = (T) context.getInstantiatorFor(targetEntity)
				.createInstance(targetEntity, new ParameterValueProvider() {
					@Override
					public Object getParameterValue(Parameter parameter) {
						PersistentProperty<?> targetProperty = targetEntity.getPersistentProperty(parameter.getName());
						if (targetProperty == null) {
							throw new MappingException("Cannot map constructor parameter " + parameter.getName()
									+ " to a property of class " + targetEntityType);
						}
						return getPropertyValueFor(targetProperty, sourceEntity, sourceAccessor);
					}
				});

		PersistentPropertyAccessor<Object> dtoAccessor = targetEntity.getPropertyAccessor(entity);
		targetEntity.doWithProperties((SimplePropertyHandler) property -> {

			if (constructor.isConstructorParameter(property)) {
				return;
			}

			Object propertyValue = getPropertyValueFor(property, sourceEntity, sourceAccessor);
			dtoAccessor.setProperty(property, propertyValue);
		});

		return entity;
	}

	@Nullable
	Object getPropertyValueFor(PersistentProperty<?> targetProperty, PersistentEntity<?, ?> sourceEntity,
			PersistentPropertyAccessor<?> sourceAccessor) {

		String targetPropertyName = targetProperty.getName();
		PersistentProperty<?> sourceProperty = sourceEntity.getPersistentProperty(targetPropertyName);
		if (sourceProperty != null) {
			return sourceAccessor.getProperty(sourceProperty);
		}

		return null;
	}
}
