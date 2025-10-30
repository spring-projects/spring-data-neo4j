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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.callback.EntityCallbacks;
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
import org.springframework.data.neo4j.core.mapping.callback.EventSupport;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.util.Lazy;
import org.springframework.util.ReflectionUtils;

/**
 * An implementation of both a {@link Schema} as well as a Neo4j version of Spring Data's
 * {@link org.springframework.data.mapping.context.MappingContext}. It is recommended to
 * provide the initial set of classes through {@link #setInitialEntitySet(Set)}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty>
		implements Schema {

	/**
	 * The shared entity instantiators of this context. Those should not be recreated for
	 * each entity or even not for each query, as otherwise the cache of Spring's
	 * org.springframework.data.convert.ClassGeneratingEntityInstantiator won't apply
	 */
	private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

	private static final Set<Class<?>> VOID_TYPES = new HashSet<>(Arrays.asList(Void.class, void.class));

	/**
	 * A map of fallback id generators, that have not been added to the application
	 * context.
	 */
	private final Map<Class<? extends IdGenerator<?>>, IdGenerator<?>> idGenerators = new ConcurrentHashMap<>();

	private final Map<Class<? extends Neo4jPersistentPropertyConverterFactory>, Neo4jPersistentPropertyConverterFactory> converterFactories = new ConcurrentHashMap<>();

	/**
	 * The {@link NodeDescriptionStore} is basically a {@link Map} and it is used to break
	 * the dependency cycle between this class and the
	 * {@link DefaultNeo4jEntityConverter}.
	 */
	private final NodeDescriptionStore nodeDescriptionStore = new NodeDescriptionStore();

	private final TypeSystem typeSystem;

	private final Neo4jConversionService conversionService;

	private final Map<Neo4jPersistentEntity<?>, Set<MethodHolder>> postLoadMethods = new ConcurrentHashMap<>();

	private final Lazy<PersistentPropertyCharacteristicsProvider> propertyCharacteristicsProvider;

	private EventSupport eventSupport;

	@Nullable
	private AutowireCapableBeanFactory beanFactory;

	private boolean strict = false;

	public Neo4jMappingContext() {

		this(new Builder());
	}

	public Neo4jMappingContext(Neo4jConversions neo4jConversions) {

		this(new Builder(neo4jConversions, null, null));
	}

	private Neo4jMappingContext(Builder builder) {

		this.conversionService = new DefaultNeo4jConversionService(builder.neo4jConversions);
		this.typeSystem = (builder.typeSystem != null) ? builder.typeSystem : TypeSystem.getDefault();
		this.eventSupport = EventSupport.useExistingCallbacks(this, EntityCallbacks.create());

		super.setSimpleTypeHolder(builder.neo4jConversions.getSimpleTypeHolder());

		PersistentPropertyCharacteristicsProvider characteristicsProvider = builder.persistentPropertyCharacteristicsProvider;
		this.propertyCharacteristicsProvider = Lazy
			.of(() -> (characteristicsProvider != null || this.beanFactory == null) ? characteristicsProvider
					: this.beanFactory.getBeanProvider(PersistentPropertyCharacteristicsProvider.class).getIfUnique());
	}

	public static Builder builder() {
		return new Builder();
	}

	private static boolean isValidParentNode(Class<?> parentClass) {
		if (parentClass == null || parentClass.equals(Object.class)) {
			return false;
		}

		// Either a concrete class explicitly annotated as Node or an abstract class
		return Modifier.isAbstract(parentClass.getModifiers()) || parentClass.isAnnotationPresent(Node.class);
	}

	private static boolean isValidEntityInterface(Class<?> typeInterface) {
		return typeInterface.isAnnotationPresent(Node.class);
	}

	private static Set<MethodHolder> computePostLoadMethods(Neo4jPersistentEntity<?> entity) {

		Set<MethodHolder> postLoadMethods = new LinkedHashSet<>();
		ReflectionUtils.MethodFilter isValidPostLoad = method -> {
			int modifiers = method.getModifiers();
			return !Modifier.isStatic(modifiers) && method.getParameterCount() == 0
					&& VOID_TYPES.contains(method.getReturnType())
					&& AnnotationUtils.findAnnotation(method, PostLoad.class) != null;
		};
		Class<?> underlyingClass = entity.getUnderlyingClass();
		ReflectionUtils.doWithMethods(underlyingClass, method -> postLoadMethods.add(new MethodHolder(method, null)),
				isValidPostLoad);
		if (KotlinDetector.isKotlinType(underlyingClass)) {
			ReflectionUtils.doWithFields(underlyingClass, field -> {
				ReflectionUtils.doWithMethods(field.getType(),
						method -> postLoadMethods.add(new MethodHolder(method, field)), isValidPostLoad);
			}, field -> field.isSynthetic() && field.getName().startsWith("$$delegate_"));
		}

		return Collections.unmodifiableSet(postLoadMethods);
	}

	/**
	 * We need to set the context to non-strict in case we must dynamically add parent
	 * classes. As there is no way to access the original value without reflection, we
	 * track its change.
	 * @param strict the new value for the strict setting
	 */
	@Override
	public void setStrict(boolean strict) {
		super.setStrict(strict);
		this.strict = strict;
	}

	@Override
	public Neo4jEntityConverter getEntityConverter() {
		return new DefaultNeo4jEntityConverter(INSTANTIATORS, this.nodeDescriptionStore, this.conversionService,
				this.eventSupport, this.typeSystem);
	}

	public Neo4jConversionService getConversionService() {
		return this.conversionService;
	}

	public EntityInstantiator getInstantiatorFor(PersistentEntity<?, ?> entity) {
		return INSTANTIATORS.getInstantiatorFor(entity);
	}

	public boolean hasCustomWriteTarget(Class<?> targetType) {
		return this.conversionService.hasCustomWriteTarget(targetType);
	}

	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		final DefaultNeo4jPersistentEntity<T> newEntity = new DefaultNeo4jPersistentEntity<>(typeInformation);
		String primaryLabel = newEntity.getPrimaryLabel();

		// We don't store interface in the index.
		// This is required for a pretty standard scenario: Having the interface spotting
		// the standard name of a domain
		// as the class name, and different implementations (for example even in different
		// stores) having a store dedicated
		// annotation repeating the interface name
		if (!newEntity.describesInterface()) {
			if (this.nodeDescriptionStore.containsKey(primaryLabel)) {

				Neo4jPersistentEntity<?> existingEntity = (Neo4jPersistentEntity<?>) this.nodeDescriptionStore
					.get(primaryLabel);
				if (existingEntity != null && !existingEntity.getTypeInformation()
					.getRawTypeInformation()
					.equals(typeInformation.getRawTypeInformation())) {
					String message = String.format(Locale.ENGLISH,
							"The schema already contains a node description under the primary label %s", primaryLabel);
					throw new MappingException(message);
				}
			}

			if (this.nodeDescriptionStore.containsValue(newEntity)) {
				Optional<String> label = this.nodeDescriptionStore.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(newEntity))
					.map(Map.Entry::getKey)
					.findFirst();

				String message = String.format(Locale.ENGLISH,
						"The schema already contains description %s under the primary label %s", newEntity,
						label.orElse("n/a"));
				throw new MappingException(message);
			}

			NodeDescription<?> existingDescription = this.getNodeDescription(newEntity.getUnderlyingClass());
			if (existingDescription != null) {

				if (!existingDescription.getPrimaryLabel().equals(newEntity.getPrimaryLabel())) {
					String message = String.format(Locale.ENGLISH,
							"The schema already contains description with the underlying class %s under the primary label %s",
							newEntity.getUnderlyingClass().getName(), existingDescription.getPrimaryLabel());
					throw new MappingException(message);
				}
			}

			this.nodeDescriptionStore.put(primaryLabel, newEntity);
		}

		// determine super class to create the node hierarchy
		Class<T> type = typeInformation.getType();
		Class<? super T> superclass = type.getSuperclass();

		if (isValidParentNode(superclass)) {
			synchronized (this) {
				super.setStrict(false);
				Neo4jPersistentEntity<?> parentNodeDescription = getPersistentEntity(superclass);
				if (parentNodeDescription != null) {
					parentNodeDescription.addChildNodeDescription(newEntity);
					newEntity.setParentNodeDescription(parentNodeDescription);
				}
				this.setStrict(this.strict);
			}
		}

		for (Class<?> typeInterface : type.getInterfaces()) {
			if (isValidEntityInterface(typeInterface)) {
				super.setStrict(false);
				Neo4jPersistentEntity<?> parentNodeDescription = getPersistentEntity(typeInterface);
				if (parentNodeDescription != null) {
					parentNodeDescription.addChildNodeDescription(newEntity);
				}
				this.setStrict(this.strict);
			}
		}

		return newEntity;
	}

	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property, Neo4jPersistentEntity<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		PersistentPropertyCharacteristics optionalCharacteristics = this.propertyCharacteristicsProvider.getOptional()
			.flatMap(provider -> Optional.ofNullable(provider.apply(property, owner)))
			.orElse(null);

		return new DefaultNeo4jPersistentProperty(property, owner, this, simpleTypeHolder, optionalCharacteristics);
	}

	@Override
	@Nullable public NodeDescription<?> getNodeDescription(String primaryLabel) {
		return this.nodeDescriptionStore.get(primaryLabel);
	}

	@Override
	@Nullable public NodeDescription<?> getNodeDescription(Class<?> underlyingClass) {
		return doGetPersistentEntity(underlyingClass);
	}

	@Override
	@Nullable public Neo4jPersistentEntity<?> getPersistentEntity(TypeInformation<?> typeInformation) {

		Neo4jPersistentEntity<?> existingDescription = this.doGetPersistentEntity(typeInformation);
		if (existingDescription != null) {
			return existingDescription;
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

	@Nullable private Neo4jPersistentEntity<?> doGetPersistentEntity(TypeInformation<?> typeInformation) {
		return doGetPersistentEntity(typeInformation.getRawTypeInformation().getType());
	}

	/**
	 * This checks whether a type is an interface and if so, tries to figure whether a
	 * persistent entity exists matching the name that can be derived from the interface.
	 * If the interface is assignable by the class by behind the retrieved entity, that
	 * entity will be used. Otherwise we will look for an entity matching the interface
	 * type itself.
	 * @param underlyingClass the underlying class
	 * @return an optional persistent entity
	 */
	@Nullable private Neo4jPersistentEntity<?> doGetPersistentEntity(Class<?> underlyingClass) {

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
		}
		else {
			idGenerator = this.beanFactory.getBeanProvider(t).getIfUnique(() -> {
				// The beanFactory can't actually be reassigned, so doing a whole double
				// lock check is a bit overkill
				@SuppressWarnings("NullAway")
				var result = this.beanFactory.createBean(t);
				return result;
			});
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
		}
		catch (NoSuchBeanDefinitionException ex) {
			return Optional.empty();
		}
	}

	@Nullable Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
		try {
			return ReflectionUtils.accessibleConstructor(clazz, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private <T extends Neo4jPersistentPropertyConverterFactory> T getOrCreateConverterFactoryOfType(
			Class<T> converterFactoryType) {

		return converterFactoryType.cast(this.converterFactories.computeIfAbsent(converterFactoryType, t -> {
			Constructor<?> optionalConstructor;
			optionalConstructor = findConstructor(t, BeanFactory.class, Neo4jConversionService.class);
			if (optionalConstructor != null) {
				return t
					.cast(BeanUtils.instantiateClass(optionalConstructor, this.beanFactory, this.conversionService));
			}

			optionalConstructor = findConstructor(t, Neo4jConversionService.class, BeanFactory.class);
			if (optionalConstructor != null) {
				return t
					.cast(BeanUtils.instantiateClass(optionalConstructor, this.beanFactory, this.conversionService));
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

	@Nullable Neo4jPersistentPropertyConverter<?> getOptionalCustomConversionsFor(Neo4jPersistentProperty persistentProperty) {

		// Is the annotation present at all?
		if (!persistentProperty.isAnnotationPresent(ConvertWith.class)) {
			return null;
		}

		ConvertWith convertWith = persistentProperty.getRequiredAnnotation(ConvertWith.class);
		Neo4jPersistentPropertyConverterFactory persistentPropertyConverterFactory = this
			.getOrCreateConverterFactoryOfType(convertWith.converterFactory());
		Neo4jPersistentPropertyConverter<?> customConverter = persistentPropertyConverterFactory
			.getPropertyConverterFor(persistentProperty);

		boolean forCollection = false;
		if (persistentProperty.isCollectionLike()) {
			Class<?> converterClass;
			Method getClassOfDelegate = ReflectionUtils.findMethod(customConverter.getClass(), "getClassOfDelegate");
			if (getClassOfDelegate != null) {
				ReflectionUtils.makeAccessible(getClassOfDelegate);
				converterClass = (Class<?>) ReflectionUtils.invokeMethod(getClassOfDelegate, customConverter);
			}
			else {
				converterClass = customConverter.getClass();
			}
			Map<String, Type> typeVariableMap = (converterClass != null)
					? GenericTypeResolver.getTypeVariableMap(converterClass)
						.entrySet()
						.stream()
						.collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue))
					: Map.of();
			Type propertyType = null;
			if (typeVariableMap.containsKey("T")) {
				propertyType = typeVariableMap.get("T");
			}
			else if (typeVariableMap.containsKey("P")) {
				propertyType = typeVariableMap.get("P");
			}
			forCollection = propertyType instanceof ParameterizedType
					&& persistentProperty.getType().equals(((ParameterizedType) propertyType).getRawType());
		}

		return new NullSafeNeo4jPersistentPropertyConverter<>(customConverter, persistentProperty.isComposite(),
				forCollection);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		this.eventSupport = EventSupport.discoverCallbacks(this, this.beanFactory);
	}

	public CreateRelationshipStatementHolder createStatementForImperativeSimpleRelationshipBatch(
			Neo4jPersistentEntity<?> neo4jPersistentEntity, RelationshipDescription relationshipDescription,
			List<Object> plainRelationshipRows, boolean canUseElementId) {

		return createStatementForSingleRelationship(neo4jPersistentEntity,
				(DefaultRelationshipDescription) relationshipDescription, plainRelationshipRows, canUseElementId);
	}

	public CreateRelationshipStatementHolder createStatementForImperativeRelationshipsWithPropertiesBatch(boolean isNew,
			Neo4jPersistentEntity<?> neo4jPersistentEntity, RelationshipDescription relationshipDescription,
			Object relatedValues, List<Map<String, Object>> relationshipPropertiesRows, boolean canUseElementId) {

		List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipPropertyValues = ((Collection<?>) relatedValues)
			.stream()
			.map(MappingSupport.RelationshipPropertiesWithEntityHolder.class::cast)
			.collect(Collectors.toList());

		return createStatementForRelationshipWithPropertiesBatch(isNew, neo4jPersistentEntity, relationshipDescription,
				relationshipPropertyValues, relationshipPropertiesRows, canUseElementId);
	}

	public CreateRelationshipStatementHolder createStatementForSingleRelationship(
			Neo4jPersistentEntity<?> neo4jPersistentEntity, RelationshipDescription relationshipContext,
			Object relatedValue, boolean isNewRelationship, boolean canUseElementId) {

		if (relationshipContext.hasRelationshipProperties()) {
			MappingSupport.RelationshipPropertiesWithEntityHolder relatedValueEntityHolder = (MappingSupport.RelationshipPropertiesWithEntityHolder) (
			// either this is a scalar entity holder value
			// or a dynamic relationship with
			// either a list of entity holders
			// or a scalar value
			(relatedValue instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) ? relatedValue
					: (((Map.Entry<?, ?>) relatedValue).getValue() instanceof List)
							? ((List<?>) ((Map.Entry<?, ?>) relatedValue).getValue()).get(0)
							: ((Map.Entry<?, ?>) relatedValue).getValue());

			String dynamicRelationshipType = null;
			if (relationshipContext.isDynamic()) {
				Neo4jPersistentProperty inverse = ((DefaultRelationshipDescription) relationshipContext).getInverse();
				TypeInformation<?> keyType = inverse.getTypeInformation().getRequiredComponentType();
				Object key = ((Map.Entry) relatedValue).getKey();
				dynamicRelationshipType = this.conversionService
					.writeValue(key, keyType, inverse.getOptionalConverter())
					.asString();
			}
			return createStatementForRelationshipWithProperties(neo4jPersistentEntity, relationshipContext,
					dynamicRelationshipType, relatedValueEntityHolder, isNewRelationship, canUseElementId);
		}
		else {
			return createStatementForSingleRelationship(neo4jPersistentEntity,
					(DefaultRelationshipDescription) relationshipContext, relatedValue, canUseElementId);
		}
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithProperties(
			Neo4jPersistentEntity<?> neo4jPersistentEntity, RelationshipDescription relationshipDescription,
			@Nullable String dynamicRelationshipType,
			MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue, boolean isNewRelationship,
			boolean canUseElementId) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationshipWithProperties(
				neo4jPersistentEntity, relationshipDescription, isNewRelationship, dynamicRelationshipType,
				canUseElementId, false);

		Map<String, Object> propMap = new HashMap<>();
		// write relationship properties
		getEntityConverter().write(relatedValue.getRelationshipProperties(), propMap);

		return new CreateRelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithPropertiesBatch(boolean isNew,
			Neo4jPersistentEntity<?> neo4jPersistentEntity, RelationshipDescription relationshipDescription,
			List<MappingSupport.RelationshipPropertiesWithEntityHolder> relatedValues,
			List<Map<String, Object>> relationshipPropertiesRows, boolean canUseElementId) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareUpdateOfRelationshipsWithProperties(
				neo4jPersistentEntity, relationshipDescription, isNew, canUseElementId);
		List<Object> relationshipRows = new ArrayList<>();
		Map<String, Object> relationshipPropertiesEntries = new HashMap<>();
		if (isNew) {
			for (int i = 0; i < relatedValues.size(); i++) {
				MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue = relatedValues.get(i);
				// write relationship properties
				Map<String, Object> propMap = relationshipPropertiesRows.get(i);
				getEntityConverter().write(relatedValue.getRelationshipProperties(), propMap);
				relationshipRows.add(propMap);
			}
		}
		relationshipPropertiesEntries.put(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, relationshipRows);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery, relationshipPropertiesEntries);
	}

	private CreateRelationshipStatementHolder createStatementForSingleRelationship(
			Neo4jPersistentEntity<?> neo4jPersistentEntity, DefaultRelationshipDescription relationshipDescription,
			Object relatedValue, boolean canUseElementId) {

		String relationshipType;
		if (!relationshipDescription.isDynamic()) {
			relationshipType = null;
		}
		else {
			Neo4jPersistentProperty inverse = relationshipDescription.getInverse();
			TypeInformation<?> keyType = inverse.getTypeInformation().getRequiredComponentType();
			Object key = ((Map.Entry<?, ?>) relatedValue).getKey();
			relationshipType = this.conversionService.writeValue(key, keyType, inverse.getOptionalConverter())
				.asString();
		}

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationships(neo4jPersistentEntity,
				relationshipDescription, relationshipType, canUseElementId);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery, Collections.emptyMap());
	}

	/**
	 * Executes all post load methods of the given instance.
	 * @param entity the entity definition
	 * @param instance the instance whose post load methods should be executed
	 * @param <T> type of the entity
	 * @return the instance
	 */
	public <T> T invokePostLoad(Neo4jPersistentEntity<T> entity, T instance) {

		getPostLoadMethods(entity).forEach(methodHolder -> methodHolder.invoke(instance));
		return instance;
	}

	Set<MethodHolder> getPostLoadMethods(Neo4jPersistentEntity<?> entity) {
		return this.postLoadMethods.computeIfAbsent(entity, Neo4jMappingContext::computePostLoadMethods);
	}

	/**
	 * A builder for creating custom instances of a {@link Neo4jMappingContext}.
	 *
	 * @since 6.3.7
	 */
	public static final class Builder {

		private Neo4jConversions neo4jConversions;

		private TypeSystem typeSystem;

		@Nullable
		private PersistentPropertyCharacteristicsProvider persistentPropertyCharacteristicsProvider;

		private Builder() {
			this(new Neo4jConversions(), null, null);
		}

		private Builder(Neo4jConversions neo4jConversions, @Nullable TypeSystem typeSystem,
				@Nullable PersistentPropertyCharacteristicsProvider persistentPropertyCharacteristicsProvider) {
			this.neo4jConversions = neo4jConversions;
			this.typeSystem = Objects.requireNonNullElseGet(typeSystem, TypeSystem::getDefault);
			this.persistentPropertyCharacteristicsProvider = persistentPropertyCharacteristicsProvider;
		}

		@SuppressWarnings("HiddenField")
		public Builder withNeo4jConversions(Neo4jConversions neo4jConversions) {
			this.neo4jConversions = neo4jConversions;
			return this;
		}

		@SuppressWarnings("HiddenField")
		public Builder withPersistentPropertyCharacteristicsProvider(
				PersistentPropertyCharacteristicsProvider persistentPropertyCharacteristicsProvider) {
			this.persistentPropertyCharacteristicsProvider = persistentPropertyCharacteristicsProvider;
			return this;
		}

		@SuppressWarnings("HiddenField")
		public Builder withTypeSystem(TypeSystem typeSystem) {
			this.typeSystem = Objects.requireNonNullElseGet(typeSystem, TypeSystem::getDefault);
			return this;
		}

		public Neo4jMappingContext build() {
			return new Neo4jMappingContext(this);
		}

	}

	static class MethodHolder {

		private final Method method;

		@Nullable
		private final Field delegate;

		MethodHolder(Method method, @Nullable Field delegate) {
			this.method = method;
			this.delegate = delegate;
		}

		static Object getInstanceOrDelegate(Object instance, @Nullable Field delegateHolder) {
			if (delegateHolder == null) {
				return instance;
			}
			else {
				try {
					delegateHolder.setAccessible(true);
					return delegateHolder.get(instance);
				}
				catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		String getName() {
			return this.method.getName();
		}

		void invoke(Object instance) {

			this.method.setAccessible(true);
			ReflectionUtils.invokeMethod(this.method, getInstanceOrDelegate(instance, this.delegate));
		}

	}

}
