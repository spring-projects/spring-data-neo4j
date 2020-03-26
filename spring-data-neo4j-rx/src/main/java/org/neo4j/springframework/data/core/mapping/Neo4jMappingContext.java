/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.mapping;

import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.neo4j.springframework.data.core.convert.Neo4jConverter;
import org.neo4j.springframework.data.core.convert.Neo4jSimpleTypes;
import org.neo4j.springframework.data.core.schema.IdGenerator;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Schema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * An implementation of both a {@link Schema} as well as a Neo4j version of Spring Data's
 * {@link org.springframework.data.mapping.context.MappingContext}. It is recommended to provide
 * the initial set of classes through {@link #setInitialEntitySet(Set)}.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Neo4jMappingContext
	extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> implements Schema {

	/**
	 * A map of fallback id generators, that have not been added to the application context
	 */
	private final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> idGenerators = new ConcurrentHashMap<>();

	/**
	 * The {@link NodeDescriptionStore} is basically a {@link Map} and it is used to break the dependency
	 * cycle between this class and the {@link DefaultNeo4jConverter}.
	 */
	private final NodeDescriptionStore nodeDescriptionStore = new NodeDescriptionStore();

	/**
	 * The converter used in this mapping context.
	 */
	private final Neo4jConverter converter;

	private @Nullable AutowireCapableBeanFactory beanFactory;

	public Neo4jMappingContext() {

		this(new Neo4jConversions());
	}

	public Neo4jMappingContext(Neo4jConversions neo4jConversions) {

		super.setSimpleTypeHolder(Neo4jSimpleTypes.HOLDER);
		this.converter = new DefaultNeo4jConverter(neo4jConversions, nodeDescriptionStore);
	}

	public Neo4jConverter getConverter() {
		return converter;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		final DefaultNeo4jPersistentEntity<T> newEntity = new DefaultNeo4jPersistentEntity<>(typeInformation);
		String primaryLabel = newEntity.getPrimaryLabel();

		if (this.nodeDescriptionStore.containsKey(primaryLabel)) {
			// @formatter:off
			throw new MappingException(
				String.format(Locale.ENGLISH, "The schema already contains a node description under the primary label %s",
						primaryLabel));
			// @formatter:on
		}

		if (this.nodeDescriptionStore.containsValue(newEntity)) {
			Optional<String> label = this.nodeDescriptionStore.entrySet().stream()
				.filter(e -> e.getValue().equals(newEntity)).map(
					Map.Entry::getKey).findFirst();

			throw new MappingException(
				String.format(Locale.ENGLISH, "The schema already contains description %s under the primary label %s",
					newEntity, label.orElse("n/a")));
		}

		NodeDescription<?> existingDescription = this.getNodeDescription(newEntity.getUnderlyingClass());
		if (existingDescription != null) {

			throw new MappingException(String.format(Locale.ENGLISH,
				"The schema already contains description with the underlying class %s under the primary label %s",
				newEntity.getUnderlyingClass().getName(), existingDescription.getPrimaryLabel()));
		}

		this.nodeDescriptionStore.put(primaryLabel, newEntity);

		// child class after parent class
		Class<? super T> superclass = typeInformation.getType().getSuperclass();
		Neo4jPersistentEntity<?> parentNodeDescription =
			(Neo4jPersistentEntity<?>) nodeDescriptionStore.getNodeDescription(superclass);

		if (isValidParentNode(parentNodeDescription)) {
			parentNodeDescription.addChildNodeDescription(newEntity);
			newEntity.setParentNodeDescription(parentNodeDescription);
		}

		// parent class after child class
		if (isValidParentNode(newEntity)) {
			Class<?> thisParentClass = typeInformation.getType();

			for (NodeDescription<?> possibleChild : nodeDescriptionStore.values()) {
				Class<?> possibleChildClass = possibleChild.getUnderlyingClass();
				if (possibleChildClass.getSuperclass().equals(thisParentClass)) {
					newEntity.addChildNodeDescription(possibleChild);
					possibleChild.setParentNodeDescription(newEntity);
				}
			}
		}

		return newEntity;
	}

	private boolean isValidParentNode(@Nullable Neo4jPersistentEntity<?> entity) {
		return entity != null && Modifier.isAbstract(entity.getType().getModifiers());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property,
		Neo4jPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {

		return new DefaultNeo4jPersistentProperty(property, owner, this, simpleTypeHolder);
	}

	@Override
	@Nullable
	public NodeDescription<?> getNodeDescription(String primaryLabel) {
		return this.nodeDescriptionStore.get(primaryLabel);
	}

	@Override
	public NodeDescription<?> getNodeDescription(Class<?> underlyingClass) {
		return this.nodeDescriptionStore.getNodeDescription(underlyingClass);
	}

	@Override
	public Optional<Neo4jPersistentEntity<?>> addPersistentEntity(Class<?> type) {
		return super.addPersistentEntity(type);
	}

	@Override
	public <T extends IdGenerator<?>> T getOrCreateIdGeneratorOfType(Class<T> idGeneratorType) {

		if (this.idGenerators.containsKey(idGeneratorType)) {
			return (T) this.idGenerators.get(idGeneratorType);
		} else {
			T idGenerator;
			if (this.beanFactory == null) {
				idGenerator = BeanUtils.instantiateClass(idGeneratorType);
			} else {
				idGenerator = this.beanFactory.getBeanProvider(idGeneratorType)
					.getIfUnique(() -> this.beanFactory.createBean(idGeneratorType));
			}
			this.idGenerators.put(idGeneratorType, idGenerator);
			return idGenerator;
		}
	}

	@Override
	public <T extends IdGenerator<?>> Optional<T> getIdGenerator(String reference) {
		try {
			return Optional.of((T) this.beanFactory.getBean(reference));
		} catch (NoSuchBeanDefinitionException e) {
			return Optional.empty();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		Driver driver = this.beanFactory.getBean(Driver.class);
		((DefaultNeo4jConverter) this.converter).setTypeSystem(driver.defaultTypeSystem());
	}
}
