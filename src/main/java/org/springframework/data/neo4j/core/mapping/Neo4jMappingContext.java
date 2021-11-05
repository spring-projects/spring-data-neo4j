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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * An implementation of both a {@link Schema} as well as a Neo4j version of Spring Data's
 * {@link org.springframework.data.mapping.context.MappingContext}. It is recommended to provide the initial set of
 * classes through {@link #setInitialEntitySet(Set)}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty>
		implements Schema {

	/**
	 * The shared entity instantiators of this context. Those should not be recreated for each entity or even not for each
	 * query, as otherwise the cache of Spring's org.springframework.data.convert.ClassGeneratingEntityInstantiator won't
	 * apply
	 */
	private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

	/**
	 * A map of fallback id generators, that have not been added to the application context
	 */
	private final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> idGenerators = new ConcurrentHashMap<>();

	private final Map<Class<? extends Neo4jPersistentPropertyConverterFactory>, Neo4jPersistentPropertyConverterFactory> converterFactories = new ConcurrentHashMap<>();

	/**
	 * The {@link NodeDescriptionStore} is basically a {@link Map} and it is used to break the dependency cycle between
	 * this class and the {@link DefaultNeo4jEntityConverter}.
	 */
	private final NodeDescriptionStore nodeDescriptionStore = new NodeDescriptionStore();

	private final TypeSystem typeSystem;

	private final Neo4jConversionService conversionService;

	private @Nullable AutowireCapableBeanFactory beanFactory;

	private boolean strict = false;

	public Neo4jMappingContext() {

		this(new Neo4jConversions());
	}

	public Neo4jMappingContext(Neo4jConversions neo4jConversions) {

		this(neo4jConversions, null);
	}

	/**
	 * We need to set the context to non-strict in case we must dynamically add parent classes. As there is no
	 * way to access the original value without reflection, we track its change.
	 *
	 * @param strict The new value for the strict setting.
	 */
	@Override
	public void setStrict(boolean strict) {
		super.setStrict(strict);
		this.strict = strict;
	}

	/**
	 * This API is primarily used from inside the CDI extension to configure the type system. This is necessary as
	 * we don't get notified of the context via {@link #setApplicationContext(ApplicationContext applicationContext)}.
	 *
	 * @param neo4jConversions The conversions to be used
	 * @param typeSystem       The current drivers type system. If this is null, we use the default one without accessing the driver.
	 */
	@API(status = API.Status.INTERNAL, since = "6.0")
	public Neo4jMappingContext(Neo4jConversions neo4jConversions, @Nullable TypeSystem typeSystem) {

		this.conversionService = new DefaultNeo4jConversionService(neo4jConversions);
		this.typeSystem = typeSystem == null ? InternalTypeSystem.TYPE_SYSTEM : typeSystem;

		super.setSimpleTypeHolder(neo4jConversions.getSimpleTypeHolder());
	}

	public Neo4jEntityConverter getEntityConverter() {
		return new DefaultNeo4jEntityConverter(INSTANTIATORS, conversionService, nodeDescriptionStore, typeSystem);
	}

	public Neo4jConversionService getConversionService() {
		return conversionService;
	}

	public EntityInstantiator getInstantiatorFor(PersistentEntity<?, ?> entity) {
		return INSTANTIATORS.getInstantiatorFor(entity);
	}

	public boolean hasCustomWriteTarget(Class<?> targetType) {
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

		// We don't store interface in the index.
		// This is required for a pretty standard scenario: Having the interface spotting the standard name of a domain
		// as the class name, and different implementations (for example even in different stores) having a store dedicated
		// annotation repeating the interface name
		if (!newEntity.describesInterface()) {
			if (this.nodeDescriptionStore.containsKey(primaryLabel)) {

				Neo4jPersistentEntity existingEntity = (Neo4jPersistentEntity) this.nodeDescriptionStore.get(primaryLabel);
				if (!existingEntity.getTypeInformation().getRawTypeInformation().equals(typeInformation.getRawTypeInformation())) {
					String message = String.format(Locale.ENGLISH, "The schema already contains a node description under the primary label %s", primaryLabel);
					throw new MappingException(message);
				}
			}

			if (this.nodeDescriptionStore.containsValue(newEntity)) {
				Optional<String> label = this.nodeDescriptionStore.entrySet().stream().filter(e -> e.getValue().equals(newEntity)).map(Map.Entry::getKey).findFirst();

				String message = String.format(Locale.ENGLISH, "The schema already contains description %s under the primary label %s", newEntity, label.orElse("n/a"));
				throw new MappingException(message);
			}

			NodeDescription<?> existingDescription = this.getNodeDescription(newEntity.getUnderlyingClass());
			if (existingDescription != null) {

				if (!existingDescription.getPrimaryLabel().equals(newEntity.getPrimaryLabel())) {
					String message = String.format(Locale.ENGLISH, "The schema already contains description with the underlying class %s under the primary label %s", newEntity.getUnderlyingClass().getName(), existingDescription.getPrimaryLabel());
					throw new MappingException(message);
				}
			}

			this.nodeDescriptionStore.put(primaryLabel, newEntity);
		}

		// determine super class to create the node hierarchy
		Class<? super T> superclass = typeInformation.getType().getSuperclass();

		if (isValidParentNode(superclass)) {
			synchronized (this) {
				super.setStrict(false);
				Neo4jPersistentEntity<?> parentNodeDescription = getPersistentEntity(superclass);
				if (parentNodeDescription != null) {
					parentNodeDescription.addChildNodeDescription(newEntity);
					newEntity.setParentNodeDescription(parentNodeDescription);
				}
				this.setStrict(strict);
			}
		}

		return newEntity;
	}

	private static boolean isValidParentNode(@Nullable Class<?> parentClass) {
		if (parentClass == null || parentClass.equals(Object.class)) {
			return false;
		}

		// Either a concrete class explicitly annotated as Node or an abstract class
		return Modifier.isAbstract(parentClass.getModifiers()) ||
				parentClass.isAnnotationPresent(Node.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property, Neo4jPersistentEntity<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		return new DefaultNeo4jPersistentProperty(property, owner, this, simpleTypeHolder);
	}

	@Override
	@Nullable
	public NodeDescription<?> getNodeDescription(String primaryLabel) {
		return this.nodeDescriptionStore.get(primaryLabel);
	}

	@Override
	public NodeDescription<?> getNodeDescription(Class<?> underlyingClass) {
		return doGetPersistentEntity(underlyingClass);
	}

	@Override
	@Nullable
	public Neo4jPersistentEntity<?> getPersistentEntity(TypeInformation<?> typeInformation) {

		NodeDescription<?> existingDescription = this.doGetPersistentEntity(typeInformation);
		if (existingDescription != null) {
			return (Neo4jPersistentEntity<?>) existingDescription;
		}
		return super.getPersistentEntity(typeInformation);
	}

	@Override
	public Optional<Neo4jPersistentEntity<?>> addPersistentEntity(TypeInformation<?> typeInformation) {

		NodeDescription<?> existingDescription = this.doGetPersistentEntity(typeInformation);
		if (existingDescription != null) {
			return Optional.of((Neo4jPersistentEntity<?>) existingDescription);
		}
		return super.addPersistentEntity(typeInformation);
	}

	/**
	 * @param typeInformation The type to retrieve a persistent entity for
	 * @return An optional persistent entity
	 * @see #doGetPersistentEntity(Class)
	 */
	@Nullable
	private Neo4jPersistentEntity<?> doGetPersistentEntity(TypeInformation<?> typeInformation) {
		return doGetPersistentEntity(typeInformation.getRawTypeInformation().getType());
	}

	/**
	 * This checks whether a type is an interface and if so, tries to figure whether a persistent entity exists
	 * matching the name that can be derived from the interface. If the interface is assignable by the class by behind
	 * the retrieved entity, that entity will be used. Otherwise we will look for an entity matching the interface type
	 * itself.
	 *
	 * @param underlyingClass The underlying class
	 * @return An optional persistent entity
	 */
	@Nullable
	private Neo4jPersistentEntity<?> doGetPersistentEntity(Class<?> underlyingClass) {

		if (underlyingClass.isInterface()) {
			String primaryLabel = DefaultNeo4jPersistentEntity.computePrimaryLabel(underlyingClass);
			Neo4jPersistentEntity<?> nodeDescription = (Neo4jPersistentEntity<?>) getNodeDescription(primaryLabel);
			if (nodeDescription != null && underlyingClass.isAssignableFrom(nodeDescription.getUnderlyingClass())) {
				return nodeDescription;
			}
		}

		return (Neo4jPersistentEntity<?>) this.nodeDescriptionStore.getNodeDescription(underlyingClass);
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

		return idGeneratorType.cast(this.idGenerators.computeIfAbsent(idGeneratorType, this::createBeanOrInstantiate));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IdGenerator<?>> Optional<T> getIdGenerator(String reference) {

		if (this.beanFactory == null) {
			return Optional.empty();
		}
		try {
			return Optional.of((T) this.beanFactory.getBean(reference));
		} catch (NoSuchBeanDefinitionException e) {
			return Optional.empty();
		}
	}

	@Nullable
	Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
		try {
			return ReflectionUtils.accessibleConstructor(clazz, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	private <T extends Neo4jPersistentPropertyConverterFactory> T getOrCreateConverterFactoryOfType(Class<T> converterFactoryType) {

		return converterFactoryType.cast(this.converterFactories.computeIfAbsent(converterFactoryType, t -> {
			Constructor<?> optionalConstructor;
			optionalConstructor = findConstructor(t, BeanFactory.class, Neo4jConversionService.class);
			if (optionalConstructor != null) {
				return t.cast(BeanUtils.instantiateClass(optionalConstructor, this.beanFactory, this.conversionService));
			}

			optionalConstructor = findConstructor(t, Neo4jConversionService.class, BeanFactory.class);
			if (optionalConstructor != null) {
				return t.cast(BeanUtils.instantiateClass(optionalConstructor, this.beanFactory, this.conversionService));
			}

			optionalConstructor = findConstructor(t, BeanFactory.class);
			if (optionalConstructor != null) {
				return t.cast(BeanUtils.instantiateClass(optionalConstructor, this.beanFactory));
			}

			optionalConstructor = findConstructor(t, Neo4jConversionService.class);
			if (optionalConstructor != null) {
				return t.cast(BeanUtils.instantiateClass(optionalConstructor, this.conversionService));
			}
			return BeanUtils.instantiateClass(t);
		}));
	}

	/**
	 * @param persistentProperty The persistent property for which the conversion should be build.
	 * @return An optional conversion.
	 */
	@Nullable
	Neo4jPersistentPropertyConverter<?> getOptionalCustomConversionsFor(Neo4jPersistentProperty persistentProperty) {

		// Is the annotation present at all?
		if (!persistentProperty.isAnnotationPresent(ConvertWith.class)) {
			return null;
		}

		ConvertWith convertWith = persistentProperty.getRequiredAnnotation(ConvertWith.class);
		Neo4jPersistentPropertyConverterFactory persistentPropertyConverterFactory = this.getOrCreateConverterFactoryOfType(convertWith.converterFactory());
		Neo4jPersistentPropertyConverter<?> customConverter = persistentPropertyConverterFactory.getPropertyConverterFor(persistentProperty);

		boolean forCollection = false;
		if (persistentProperty.isCollectionLike()) {
			Class<?> converterClass;
			Method getClassOfDelegate = ReflectionUtils.findMethod(customConverter.getClass(), "getClassOfDelegate");
			if (getClassOfDelegate != null) {
				ReflectionUtils.makeAccessible(getClassOfDelegate);
				converterClass = (Class<?>) ReflectionUtils.invokeMethod(getClassOfDelegate, customConverter);
			} else {
				converterClass = customConverter.getClass();
			}
			Map<String, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(converterClass)
					.entrySet()
					.stream()
					.collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));
			Type propertyType = null;
			if (typeVariableMap.containsKey("T")) {
				propertyType = typeVariableMap.get("T");
			} else if (typeVariableMap.containsKey("P")) {
				propertyType = typeVariableMap.get("P");
			}
			forCollection = propertyType instanceof ParameterizedType &&
							persistentProperty.getType().equals(((ParameterizedType) propertyType).getRawType());
		}

		return new NullSafeNeo4jPersistentPropertyConverter<>(customConverter, persistentProperty.isComposite(), forCollection);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
	}

	public CreateRelationshipStatementHolder createStatement(Neo4jPersistentEntity<?> neo4jPersistentEntity,
															 NestedRelationshipContext relationshipContext,
															 Object relatedValue,
															 boolean isNewRelationship) {

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

			String dynamicRelationshipType = null;
			if (relationshipContext.getRelationship().isDynamic()) {
				TypeInformation<?> keyType = relationshipContext.getInverse().getTypeInformation().getRequiredComponentType();
				Object key = ((Map.Entry) relatedValue).getKey();
				dynamicRelationshipType = conversionService.writeValue(key, keyType, relationshipContext.getInverse().getOptionalConverter()).asString();
			}
			return createStatementForRelationShipWithProperties(
					neo4jPersistentEntity, relationshipContext,
					dynamicRelationshipType, relatedValueEntityHolder, isNewRelationship
			);
		} else {
			return createStatementForRelationshipWithoutProperties(neo4jPersistentEntity, relationshipContext, relatedValue);
		}
	}

	private CreateRelationshipStatementHolder createStatementForRelationShipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, @Nullable String dynamicRelationshipType,
		    MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue, boolean isNewRelationship) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationshipWithProperties(
						neo4jPersistentEntity, relationshipContext.getRelationship(), isNewRelationship, dynamicRelationshipType);

		Map<String, Object> propMap = new HashMap<>();
		// write relationship properties
		getEntityConverter().write(relatedValue.getRelationshipProperties(), propMap);

		return new CreateRelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithoutProperties(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, Object relatedValue) {

		String relationshipType;
		if (!relationshipContext.getRelationship().isDynamic()) {
			relationshipType = null;
		} else {
			Neo4jPersistentProperty inverse = relationshipContext.getInverse();
			TypeInformation<?> keyType = inverse.getTypeInformation().getRequiredComponentType();
			Object key = ((Map.Entry<?, ?>) relatedValue).getKey();
			relationshipType = conversionService.writeValue(key, keyType, inverse.getOptionalConverter()).asString();
		}

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationship(
				neo4jPersistentEntity, relationshipContext.getRelationship(), relationshipType);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery);
	}
}
