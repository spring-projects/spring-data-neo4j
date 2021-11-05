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
package org.springframework.data.neo4j.core.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.CompositeProperty.Phase;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This annotation indicates a {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty persistent property}
 * that is composed of multiple properties on a node or relationship. The properties must share a common prefix. SDN defaults
 * to the name of the field declared on the {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity persistent entity}.
 * <p>This annotation is mainly to be used on properties of type {@link Map Map&lt;String, Object&gt;}. All values in the
 * map are subject to conversions by other registered converters. <b>Nested maps are not supported.</b>
 * <p>This annotation is the pendant to Neo4j-OGMs {@literal org.neo4j.ogm.annotation.Properties}.
 *
 * @author Michael J. Simons
 * @soundtrack Slime - Viva la Muerte
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
@ConvertWith(converterFactory = CompositePropertyConverterFactory.class)
@API(status = API.Status.STABLE, since = "6.0")
public @interface CompositeProperty {

	/**
	 * @return A converter that allows to store arbitrary objects as decomposed maps on nodes and relationships. The
	 * default converter allows only maps as composite properties.
	 */
	@AliasFor(annotation = ConvertWith.class, value = "converter")
	Class<? extends Neo4jPersistentPropertyToMapConverter> converter() default CompositeProperty.DefaultToMapConverter.class;

	@AliasFor(annotation = ConvertWith.class, value = "converterRef")
	String converterRef() default "";

	/**
	 * Allows to specify the prefix for the map properties. The default empty value instructs SDN to use the
	 * field name of the annotated property.
	 *
	 * @return The prefix used for storing the properties in the graph on the node or relationship
	 */
	String prefix() default "";

	/**
	 * Allows to specify the delimiter between prefix and map value on the properties of the node or relationship in the
	 * graph. Defaults to {@literal .}.
	 *
	 * @return Delimiter to use in the stored property names
	 */
	String delimiter() default ".";

	/**
	 * This attribute allows for configuring a transformation that is applied to the maps keys. {@link Phase#WRITE} is applied
	 * before writing the map, {@link Phase#READ} is applied on write.
	 *
	 * @return A transformation to be used on enum keys.
	 */
	Class<? extends BiFunction<Phase, String, String>> transformKeysWith() default NoopTransformation.class;

	/**
	 * The default operation for transforming the keys. Defaults to a no-op.
	 */
	final class NoopTransformation implements BiFunction<Phase, String, String> {

		@Override
		public String apply(Phase phase, String s) {
			return s;
		}
	}

	/**
	 * The default implementation, passing map properties through as they are on the way to the graph and possibly
	 * applying a post processor on the way out of the graph.
	 *
	 * @param <K> The type of the keys.
	 */
	final class DefaultToMapConverter<K> implements Neo4jPersistentPropertyToMapConverter<K, Map<K, Object>> {

		/**
		 * A post processor of the map that is eventually be stored in the entity. In case a user wishes for entities
		 * with immutable collection, that would be the place to configure it.
		 */
		private final UnaryOperator<Map<K, Object>> mapPostProcessor = UnaryOperator.identity();

		private final TypeInformation<?> typeInformationForValues;

		DefaultToMapConverter(TypeInformation<?> typeInformationForValues) {
			this.typeInformationForValues = typeInformationForValues;
		}

		@Override
		public Map<K, Value> decompose(@Nullable Map<K, Object> property, Neo4jConversionService conversionService) {

			if (property == null) {
				return Collections.emptyMap();
			}

			Map<K, Value> decomposed = new HashMap<>(property.size());
			property.forEach(
					(k, v) -> decomposed.put(k, conversionService.writeValue(v, typeInformationForValues, null)));
			return decomposed;
		}

		@Override
		public Map<K, Object> compose(Map<K, Value> source, Neo4jConversionService conversionService) {
			Map<K, Object> composed = new HashMap<>(source.size());
			source.forEach((k, v) -> composed.put(k, conversionService.readValue(v, typeInformationForValues, null)));
			return mapPostProcessor.apply(composed);
		}
	}

	/**
	 * Phase of the mapping currently taking place.
	 */
	enum Phase {
		/**
		 * Writing to the graph.
		 */
		WRITE,

		/**
		 * Graph properties are mapped to key/values of a map contained in an entity.
		 */
		READ
	}
}

/**
 * Dedicated and highly specialized converter for reading and writing {@link Map} with either enum or string keys
 * into multiple properties of Nodes or Relationships inside the Neo4j database. This is an internal API only.
 *
 * @param <K> The type of the key
 */
final class CompositePropertyConverter<K, P> implements Neo4jPersistentPropertyConverter<P> {

	private final Neo4jPersistentPropertyToMapConverter<K, P> delegate;

	private final String prefixWithDelimiter;

	private final Neo4jConversionService neo4jConversionService;

	private final Class<?> typeOfKeys;

	private final Function<K, String> keyWriter;

	private final Function<String, K> keyReader;

	CompositePropertyConverter(
			Neo4jPersistentPropertyToMapConverter<K, P> delegate,
			String prefixWithDelimiter,
			Neo4jConversionService neo4jConversionService,
			Class<?> typeOfKeys,
			Function<K, String> keyWriter,
			Function<String, K> keyReader
	) {
		this.delegate = delegate;
		this.prefixWithDelimiter = prefixWithDelimiter;
		this.neo4jConversionService = neo4jConversionService;
		this.typeOfKeys = typeOfKeys;
		this.keyWriter = keyWriter;
		this.keyReader = keyReader;
	}

	@Override
	public Value write(P property) {

		Map<K, Value> source = delegate.decompose(property, neo4jConversionService);
		Map<String, Object> temp = new HashMap<>();
		source.forEach((key, value) -> temp.put(prefixWithDelimiter + keyWriter.apply(key), value));
		return Values.value(temp);
	}

	@Override
	public P read(Value source) {

		Map<K, Value> temp = new HashMap<>();
		source.keys().forEach(k -> {
			if (k.startsWith(prefixWithDelimiter)) {
				K key = keyReader.apply(k.substring(prefixWithDelimiter.length()));
				temp.put(key, source.get(k));
			}
		});
		return this.delegate.compose(temp, neo4jConversionService);
	}

	/**
	 * Internally used via reflection.
	 * @return The type of the underlying delegate.
	 */
	@SuppressWarnings("unused")
	Class<?> getClassOfDelegate() {
		return this.delegate.getClass();
	}
}

/**
 * Internal API for creating composite converters.
 */
final class CompositePropertyConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	private static final String KEY_TYPE_KEY = "K";
	private static final String PROPERTY_TYPE_KEY = "P";

	private final BeanFactory beanFactory;
	private final Neo4jConversionService conversionServiceDelegate;

	CompositePropertyConverterFactory(@Nullable BeanFactory beanFactory, @Nullable Neo4jConversionService conversionServiceDelegate) {
		this.beanFactory = beanFactory;
		this.conversionServiceDelegate = conversionServiceDelegate;
	}

	@SuppressWarnings({"raw", "unchecked"}) // Due to dynamic enum retrieval
	@Override
	public Neo4jPersistentPropertyConverter<?> getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		CompositeProperty config = persistentProperty.getRequiredAnnotation(CompositeProperty.class);
		Class<? extends Neo4jPersistentPropertyToMapConverter> delegateClass = config.converter();
		Neo4jPersistentPropertyToMapConverter<?, Map<?, Object>> delegate = null;

		if (StringUtils.hasText(config.converterRef())) {
			if (beanFactory == null) {
				throw new IllegalStateException(
						"The default composite converter factory has been configured without a bean factory and cannot use a converter from the application context.");
			}

			delegate = beanFactory.getBean(config.converterRef(), Neo4jPersistentPropertyToMapConverter.class);
			delegateClass = delegate.getClass();
		}

		Class<?> componentType;

		if (persistentProperty.isMap()) {
			componentType = persistentProperty.getComponentType();
		} else {

			if (delegateClass == CompositeProperty.DefaultToMapConverter.class) {
				throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
						+ " can only be used on Map properties without additional configuration. Was "
						+ generateLocation(
						persistentProperty));
			}

			// Avoid resolving this as long as possible.
			Map<String, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(delegateClass).entrySet()
					.stream()
					.collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));

			Assert.isTrue(typeVariableMap.containsKey(KEY_TYPE_KEY),
					() -> "SDN could not determine the key type of your toMap converter " + generateLocation(
							persistentProperty));
			Assert.isTrue(typeVariableMap.containsKey(PROPERTY_TYPE_KEY),
					() -> "SDN could not determine the property type of your toMap converter " + generateLocation(
							persistentProperty));

			Type type = typeVariableMap.get(PROPERTY_TYPE_KEY);
			if (persistentProperty.isCollectionLike() && type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				if (persistentProperty.getType().equals(pt.getRawType()) && pt.getActualTypeArguments().length == 1) {
					type = ((ParameterizedType) type).getActualTypeArguments()[0];
				}
			}

			if (persistentProperty.getActualType() != type) {
				throw new IllegalArgumentException(
						"The property type `" + typeVariableMap.get(PROPERTY_TYPE_KEY).getTypeName() + "` created by `"
								+ delegateClass.getName() + "` " + generateLocation(persistentProperty)
								+ " doesn't match the actual property type.");
			}
			componentType = (Class<?>) typeVariableMap.get(KEY_TYPE_KEY);
		}

		boolean isEnum = componentType.isEnum();
		if (!(componentType == String.class || isEnum)) {
			throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
					+ " can only be used on Map properties with a key type of String or enum. Was " + generateLocation(
					persistentProperty));
		}

		BiFunction<Phase, String, String> keyTransformation = BeanUtils.instantiateClass(config.transformKeysWith());

		Function<String, ?> keyReader;
		Function<?, String> keyWriter;
		if (isEnum) {
			keyReader = key -> Enum.valueOf(((Class<Enum>) componentType), keyTransformation.apply(Phase.READ, key));
			keyWriter = (Enum<?> key) -> keyTransformation.apply(Phase.WRITE, key.name());
		} else {
			keyReader = key -> keyTransformation.apply(Phase.READ, key);
			keyWriter = (String key) -> keyTransformation.apply(Phase.WRITE, key);
		}

		if (delegate == null) {
			if (delegateClass == CompositeProperty.DefaultToMapConverter.class) {
				delegate = new CompositeProperty.DefaultToMapConverter(ClassTypeInformation.from(persistentProperty.getActualType()));
			} else {
				delegate = BeanUtils.instantiateClass(delegateClass);
			}
		}

		String prefixWithDelimiter = persistentProperty.computePrefixWithDelimiter();
		return new CompositePropertyConverter(
				delegate, prefixWithDelimiter, conversionServiceDelegate, componentType,  keyWriter, keyReader);
	}

	private static String generateLocation(Neo4jPersistentProperty persistentProperty) {
		return "used on `" + persistentProperty.getFieldName() + "` in `" + persistentProperty.getOwner().getName()
				+ "`";
	}
}
