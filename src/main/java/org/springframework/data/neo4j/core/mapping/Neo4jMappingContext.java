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
package org.springframework.data.neo4j.core.mapping;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jSimpleTypes;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * An implementation of both a {@link Schema} as well as a Neo4j version of Spring Data's
 * {@link org.springframework.data.mapping.context.MappingContext}. It is recommended to provide the initial set of
 * classes through {@link #setInitialEntitySet(Set)}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty>
		implements Schema {

	/**
	 * A map of fallback id generators, that have not been added to the application context
	 */
	private final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> idGenerators = new ConcurrentHashMap<>();

	private final Map<Class<? extends Neo4jPersistentPropertyConverterFactory>, Neo4jPersistentPropertyConverterFactory> converterFactorys = new ConcurrentHashMap<>();

	/**
	 * The {@link NodeDescriptionStore} is basically a {@link Map} and it is used to break the dependency cycle between
	 * this class and the {@link DefaultNeo4jEntityConverter}.
	 */
	private final NodeDescriptionStore nodeDescriptionStore = new NodeDescriptionStore();

	/**
	 * The converter used in this mapping context.
	 */
	private final Neo4jEntityConverter entityConverter;

	private final Neo4jConversionService conversionService;

	private @Nullable AutowireCapableBeanFactory beanFactory;

	public Neo4jMappingContext() {

		this(new Neo4jConversions());
	}

	public Neo4jMappingContext(Neo4jConversions neo4jConversions) {

		this(neo4jConversions, null);
	}

	/**
	 * This API is primarly used from inside the CDI extension to configure the type system. This is necessary as
	 * we don't get notified of the context via {@link #setApplicationContext(ApplicationContext applicationContext)}.
	 *
	 * @param neo4jConversions The conversions to be used
	 * @param typeSystem       The current drivers typeystem
	 */
	@API(status = API.Status.INTERNAL, since = "6.0")
	public Neo4jMappingContext(Neo4jConversions neo4jConversions, TypeSystem typeSystem) {

		super.setSimpleTypeHolder(Neo4jSimpleTypes.HOLDER);
		this.conversionService = new DefaultNeo4jConversionService(neo4jConversions);

		DefaultNeo4jEntityConverter defaultNeo4jConverter = new DefaultNeo4jEntityConverter(conversionService, nodeDescriptionStore);
		if (typeSystem != null) {
			defaultNeo4jConverter.setTypeSystem(typeSystem);
		}
		this.entityConverter = defaultNeo4jConverter;
	}

	public Neo4jEntityConverter getEntityConverter() {
		return entityConverter;
	}

	public Neo4jConversionService getConversionService() {
		return conversionService;
	}

	boolean hasCustomWriteTarget(Class<?> targetType) {
		return conversionService.hasCustomWriteTarget(targetType);
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
			throw new MappingException(String.format(Locale.ENGLISH,
					"The schema already contains a node description under the primary label %s", primaryLabel));
			// @formatter:on
		}

		if (this.nodeDescriptionStore.containsValue(newEntity)) {
			Optional<String> label = this.nodeDescriptionStore.entrySet().stream().filter(e -> e.getValue().equals(newEntity))
					.map(Map.Entry::getKey).findFirst();

			throw new MappingException(String.format(Locale.ENGLISH,
					"The schema already contains description %s under the primary label %s", newEntity, label.orElse("n/a")));
		}

		NodeDescription<?> existingDescription = this.getNodeDescription(newEntity.getUnderlyingClass());
		if (existingDescription != null) {

			throw new MappingException(String.format(Locale.ENGLISH,
					"The schema already contains description with the underlying class %s under the primary label %s",
					newEntity.getUnderlyingClass().getName(), existingDescription.getPrimaryLabel()));
		}

		this.nodeDescriptionStore.put(primaryLabel, newEntity);

		// determine super class to create the node hierarchy
		Class<? super T> superclass = typeInformation.getType().getSuperclass();

		if (isValidParentNode(superclass)) {
			Neo4jPersistentEntity<?> parentNodeDescription = getPersistentEntity(superclass);
			if (parentNodeDescription != null) {
				parentNodeDescription.addChildNodeDescription(newEntity);
				newEntity.setParentNodeDescription(parentNodeDescription);
			}
		}

		return newEntity;
	}

	private boolean isValidParentNode(@Nullable Class<?> parentClass) {
		if (parentClass == null) {
			return false;
		}

		boolean isExplicitNode = parentClass.isAnnotationPresent(Node.class);
		boolean isAbstractClass = Modifier.isAbstract(parentClass.getModifiers());

		return isExplicitNode && isAbstractClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property, Neo4jPersistentEntity<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		return new DefaultNeo4jPersistentProperty(property, owner, this, simpleTypeHolder, owner.isRelationshipPropertiesEntity());
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

	private <T> T createBeanOrInstantiate(Class<T> t) {
		T idGenerator;
		if (this.beanFactory == null) {
			idGenerator = BeanUtils.instantiateClass(t);
		} else {
			idGenerator = this.beanFactory.getBeanProvider(t).getIfUnique(() -> this.beanFactory.createBean(t));
		}
		return idGenerator;
	}

	@Override
	public <T extends IdGenerator<?>> T getOrCreateIdGeneratorOfType(Class<T> idGeneratorType) {

		return (T) this.idGenerators.computeIfAbsent(idGeneratorType, this::createBeanOrInstantiate);
	}

	@Override
	public <T extends IdGenerator<?>> Optional<T> getIdGenerator(String reference) {
		try {
			return Optional.of((T) this.beanFactory.getBean(reference));
		} catch (NoSuchBeanDefinitionException e) {
			return Optional.empty();
		}
	}

	private <T extends Neo4jPersistentPropertyConverterFactory> T getOrCreateConverterFactoryOfType(Class<T> converterFactoryType) {

		return (T) this.converterFactorys.computeIfAbsent(converterFactoryType, this::createBeanOrInstantiate);
	}

	/**
	 * @param persistentProperty The persistent property for which the conversion should be build.
	 * @return An optional conversion.
	 */
	@Nullable
	Neo4jPersistentPropertyConverter getOptionalCustomConversionsFor(Neo4jPersistentProperty persistentProperty) {

		// Is the annotation present at all?
		ConvertWith convertWith = persistentProperty.findAnnotation(ConvertWith.class);
		if (convertWith == null) {
			return null;
		}

		Neo4jPersistentPropertyConverterFactory persistentPropertyConverterFactory = this.getOrCreateConverterFactoryOfType(convertWith.converterFactory());
		return persistentPropertyConverterFactory.getPropertyConverterFor(persistentProperty);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		Driver driver = this.beanFactory.getBean(Driver.class);
		((DefaultNeo4jEntityConverter) this.entityConverter).setTypeSystem(driver.defaultTypeSystem());
	}

	public CreateRelationshipStatementHolder createStatement(Neo4jPersistentEntity<?> neo4jPersistentEntity, NestedRelationshipContext relationshipContext,
			Long relatedInternalId, Object relatedValue) {

		if (relationshipContext.hasRelationshipWithProperties()) {
			MappingSupport.RelationshipPropertiesWithEntityHolder relatedValueEntityHolder =
					(MappingSupport.RelationshipPropertiesWithEntityHolder) (
							// either this is a scalar entity holder value
							// or a dynamic relationship with
							// either a list of entity holders
							// or a scalar value
							relatedValue instanceof MappingSupport.RelationshipPropertiesWithEntityHolder
									? relatedValue
									: ((Map.Entry<?, ?>) relatedValue).getValue() instanceof List
									? ((List<?>) ((Map.Entry<?, ?>) relatedValue).getValue()).get(0)
									: ((Map.Entry<?, ?>) relatedValue).getValue());

			return createStatementForRelationShipWithProperties(neo4jPersistentEntity, relationshipContext, relatedInternalId, relatedValueEntityHolder);
		} else {
			return createStatementForRelationshipWithoutProperties(neo4jPersistentEntity, relationshipContext, relatedInternalId, relatedValue);
		}
	}

	private CreateRelationshipStatementHolder createStatementForRelationShipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, Long relatedInternalId, MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.createRelationshipWithPropertiesCreationQuery(
				neo4jPersistentEntity, relationshipContext.getRelationship(), relatedInternalId);
		Map<String, Object> propMap = new HashMap<>();
		// write relationship properties
		entityConverter.write(relatedValue.getRelationshipProperties(), propMap);

		return new CreateRelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithoutProperties(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, Long relatedInternalId, Object relatedValue) {

		String relationshipType;
		if (!relationshipContext.getRelationship().isDynamic()) {
			relationshipType = null;
		} else {
			Neo4jPersistentProperty inverse = relationshipContext.getInverse();
			TypeInformation<?> keyType = inverse.getTypeInformation().getRequiredComponentType();
			Object key = ((Map.Entry<?, ?>) relatedValue).getKey();
			relationshipType = conversionService.writeValue(key, keyType, inverse.getOptionalWritingConverter()).asString();
		}

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.createRelationshipCreationQuery(
				neo4jPersistentEntity, relationshipContext.getRelationship(), relationshipType, relatedInternalId);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery);
	}
}
