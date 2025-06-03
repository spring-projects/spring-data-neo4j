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
package org.springframework.data.neo4j.core.schema;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal API for creating composite converters.
 *
 * @author Michael J. Simons
 */
final class CompositePropertyConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	private static final String KEY_TYPE_KEY = "K";

	private static final String PROPERTY_TYPE_KEY = "P";

	private final BeanFactory beanFactory;

	private final Neo4jConversionService conversionServiceDelegate;

	CompositePropertyConverterFactory(BeanFactory beanFactory, Neo4jConversionService conversionServiceDelegate) {
		this.beanFactory = beanFactory;
		this.conversionServiceDelegate = conversionServiceDelegate;
	}

	private static String generateLocation(Neo4jPersistentProperty persistentProperty) {
		return "used on `" + persistentProperty.getFieldName() + "` in `" + persistentProperty.getOwner().getName()
				+ "`";
	}

	@SuppressWarnings({ "raw", "unchecked" }) // Due to dynamic enum retrieval
	@Override
	public Neo4jPersistentPropertyConverter<?> getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		CompositeProperty config = persistentProperty.getRequiredAnnotation(CompositeProperty.class);
		Class<? extends Neo4jPersistentPropertyToMapConverter> delegateClass = config.converter();
		Neo4jPersistentPropertyToMapConverter<?, Map<?, Object>> delegate = null;

		if (StringUtils.hasText(config.converterRef())) {
			if (this.beanFactory == null) {
				throw new IllegalStateException(
						"The default composite converter factory has been configured without a bean factory and cannot use a converter from the application context");
			}

			delegate = this.beanFactory.getBean(config.converterRef(), Neo4jPersistentPropertyToMapConverter.class);
			delegateClass = delegate.getClass();
		}

		Class<?> componentType;

		if (persistentProperty.isMap()) {
			componentType = persistentProperty.getComponentType();
		}
		else {

			if (delegateClass == CompositeProperty.DefaultToMapConverter.class) {
				throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
						+ " can only be used on Map properties without additional configuration. Was "
						+ generateLocation(persistentProperty));
			}

			// Avoid resolving this as long as possible.
			Map<String, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(delegateClass)
				.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));

			Assert.isTrue(typeVariableMap.containsKey(KEY_TYPE_KEY),
					() -> "SDN could not determine the key type of your toMap converter "
							+ generateLocation(persistentProperty));
			Assert.isTrue(typeVariableMap.containsKey(PROPERTY_TYPE_KEY),
					() -> "SDN could not determine the property type of your toMap converter "
							+ generateLocation(persistentProperty));

			Type type = typeVariableMap.get(PROPERTY_TYPE_KEY);
			if (persistentProperty.isCollectionLike() && type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				if (persistentProperty.getType().equals(pt.getRawType()) && pt.getActualTypeArguments().length == 1) {
					type = ((ParameterizedType) type).getActualTypeArguments()[0];
				}
			}

			if (persistentProperty.getActualType() != type) {
				var typeName = Optional.ofNullable(type).map(Type::getTypeName).orElse("n/a");
				throw new IllegalArgumentException(
						"The property type `" + typeName + "` created by `" + delegateClass.getName() + "` "
								+ generateLocation(persistentProperty) + " doesn't match the actual property type");
			}
			componentType = (Class<?>) typeVariableMap.get(KEY_TYPE_KEY);
		}

		boolean isEnum = componentType != null && componentType.isEnum();
		if (!(componentType == String.class || isEnum)) {
			throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
					+ " can only be used on Map properties with a key type of String or enum. Was "
					+ generateLocation(persistentProperty));
		}

		BiFunction<CompositeProperty.Phase, String, String> keyTransformation = BeanUtils
			.instantiateClass(config.transformKeysWith());

		Function<String, ?> keyReader;
		Function<?, String> keyWriter;
		if (isEnum) {
			keyReader = key -> Enum.valueOf(((Class<Enum>) componentType),
					keyTransformation.apply(CompositeProperty.Phase.READ, key));
			keyWriter = (Enum<?> key) -> keyTransformation.apply(CompositeProperty.Phase.WRITE, key.name());
		}
		else {
			keyReader = key -> keyTransformation.apply(CompositeProperty.Phase.READ, key);
			keyWriter = (String key) -> keyTransformation.apply(CompositeProperty.Phase.WRITE, key);
		}

		if (delegate == null) {
			if (delegateClass == CompositeProperty.DefaultToMapConverter.class) {
				delegate = new CompositeProperty.DefaultToMapConverter(
						TypeInformation.of(persistentProperty.getActualType()));
			}
			else {
				delegate = BeanUtils.instantiateClass(delegateClass);
			}
		}

		String prefixWithDelimiter = persistentProperty.computePrefixWithDelimiter();
		return new CompositePropertyConverter(delegate, prefixWithDelimiter, this.conversionServiceDelegate,
				Objects.requireNonNull(componentType), keyWriter, keyReader);
	}

}
