/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Converter} to instantiate entity objects from DTOs
 *
 * @param <T> entity type
 */
@API(status = API.Status.INTERNAL, since = "6.1.2")
public final class EntityFromDtoInstantiatingConverter<T> implements Converter<Object, T> {

	private final Class<?> targetEntityType;
	private final Neo4jMappingContext context;

	private final Map<Class<?>, EntityFromDtoInstantiatingConverter<?>> converterCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link Converter} to instantiate Entities from DTOs.
	 *
	 * @param entityType must not be {@literal null}.
	 * @param context    must not be {@literal null}.
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

		PersistentEntity<?, ?> sourceEntity = context.addPersistentEntity(
				ClassTypeInformation.from(dtoInstance.getClass())).get();
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
		targetEntity.doWithAll(property -> {
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
		Class<?> targetPropertyType = targetProperty.getType();
		PersistentProperty<?> sourceProperty = sourceEntity.getPersistentProperty(targetPropertyName);
		Object propertyValue = null;
		if (sourceProperty != null) {
			propertyValue = sourceAccessor.getProperty(sourceProperty);
		}

		if (propertyValue == null && targetPropertyType.isPrimitive()) {
			return ReflectionUtils.getPrimitiveDefault(targetPropertyType);
		}

		if (targetProperty.isAssociation() && targetProperty.isCollectionLike()) {
			EntityFromDtoInstantiatingConverter<?> nestedConverter = converterCache.computeIfAbsent(targetProperty.getComponentType(), t -> new EntityFromDtoInstantiatingConverter<>(t, context));
			Collection<?> source = (Collection<?>) propertyValue;
			if (source == null) {
				return CollectionFactory.createCollection(targetPropertyType, 0);
			}
			Collection<Object> target = CollectionFactory.createApproximateCollection(source, source.size());
			source.stream().map(nestedConverter::convert).forEach(target::add);
			return target;
		}

		if (propertyValue != null && !targetPropertyType.isInstance(propertyValue)) {
			EntityFromDtoInstantiatingConverter<?> nestedConverter = converterCache.computeIfAbsent(targetProperty.getType(),
					t -> new EntityFromDtoInstantiatingConverter<>(t, context));
			return nestedConverter.convert(propertyValue);
		}

		return propertyValue;
	}
}
