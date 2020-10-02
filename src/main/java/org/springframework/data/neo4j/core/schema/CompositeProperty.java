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
package org.springframework.data.neo4j.core.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.BeanUtils;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.CompositeProperty.Phase;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * This annotation indicates a {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty persistent property}
 * that is composed from multiple properties on a node or relationship. The properties must share a common prefix. SDN defaults
 * to the name of the field declared on the {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity persistent entity}.
 *
 * <p>This annotation is mainly to be used on properties of type {@link Map Map&lt;String, Object&gt;}. All values in the
 * map are subject to conversions by other registered converters. <b>Nested maps are not supported.</b>
 *
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
	 * Allows to specify the prefix for the map properties. The default empty value indicates SDN to used the
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
	class NoopTransformation implements BiFunction<Phase, String, String> {

		@Override
		public String apply(Phase phase, String s) {
			return s;
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
final class CompositePropertyConverter<K> implements Neo4jPersistentPropertyConverter<Map<K, Object>> {

	protected final String prefixWithDelimiter;

	protected final Neo4jConversionService neo4jConversionService;

	protected final Class<?> typeOfKeys;

	protected final TypeInformation<?> typeInformationForValues;

	private final Function<K, String> keyWriter;

	private final Function<String, K> keyReader;

	/**
	 * A post processor of the map that is eventually be stored in the entity. In case a user wishes for entities
	 * with immutable collection, that would be the place to configure it.
	 */
	protected final UnaryOperator<Map<K, Object>> mapPostProcessor;

	CompositePropertyConverter(
			String prefixWithDelimiter,
			Neo4jConversionService neo4jConversionService,
			Class<?> typeOfKeys,
			Class<?> typeOfValues,
			Function<K, String> keyWriter,
			Function<String, K> keyReader
	) {
		this.prefixWithDelimiter = prefixWithDelimiter;
		this.neo4jConversionService = neo4jConversionService;
		this.typeOfKeys = typeOfKeys;
		this.typeInformationForValues = ClassTypeInformation.from(typeOfValues);
		this.keyWriter = keyWriter;
		this.keyReader = keyReader;
		this.mapPostProcessor = UnaryOperator.identity();
	}

	@Override
	public Value write(Map<K, Object> source) {

		Map<String, Object> temp = new HashMap<>();
		source.forEach((key, value) -> temp.put(prefixWithDelimiter + keyWriter.apply(key), neo4jConversionService.writeValue(value, typeInformationForValues, null)));
		return Values.value(temp);
	}

	@Override
	public Map<K, Object> read(Value source) {
		Map<K, Object> temp = new HashMap<>();
		source.keys().forEach(k -> {
			if (k.startsWith(prefixWithDelimiter)) {
				K key = keyReader.apply(k.substring(prefixWithDelimiter.length()));
				Object convertedValue = neo4jConversionService.readValue(source.get(k), typeInformationForValues, null);
				temp.put(key, convertedValue);
			}
		});
		return mapPostProcessor.apply(temp);
	}
}

/**
 * Internal API for creating composite converters.
 */
final class CompositePropertyConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	private final Neo4jConversionService conversionServiceDelegate;

	CompositePropertyConverterFactory(@Nullable Neo4jConversionService conversionServiceDelegate) {
		this.conversionServiceDelegate = conversionServiceDelegate;
	}

	@Override
	public Neo4jPersistentPropertyConverter getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		if (!persistentProperty.isMap()) {
			throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
					+ " can only be used on Map properties without additional configuration. " + generateLocation(
					persistentProperty));
		}

		Class<?> componentType = persistentProperty.getComponentType();
		boolean isEnum = componentType.isEnum();
		if (!(componentType == String.class || isEnum)) {
			throw new IllegalArgumentException("@" + CompositeProperty.class.getSimpleName()
					+ " can only be used on Map properties with a key type of String or enum. " + generateLocation(
					persistentProperty));
		}

		CompositeProperty config = persistentProperty.getRequiredAnnotation(CompositeProperty.class);
		BiFunction<Phase, String, String> keyTransformation = BeanUtils.instantiateClass(config.transformKeysWith());

		Function<String, ?> keyReader;
		Function<?, String> keyWriter;
		if (isEnum) {
			keyReader = key -> Enum.valueOf(((Class<Enum>) componentType), keyTransformation.apply(
					Phase.READ, key));
			keyWriter = (Enum key) -> keyTransformation.apply(Phase.WRITE, key.name());
		} else {
			keyReader = key -> keyTransformation.apply(Phase.READ, key);
			keyWriter = (String key) -> keyTransformation.apply(Phase.WRITE, key);
		}

		String prefixWithDelimiter = persistentProperty.computePrefixWithDelimiter();
		return new CompositePropertyConverter(
				prefixWithDelimiter, conversionServiceDelegate, componentType, persistentProperty.getActualType(),
				keyWriter, keyReader);
	}

	private static String generateLocation(Neo4jPersistentProperty persistentProperty) {
		return "Was used on `" + persistentProperty.getFieldName() + "` in `" + persistentProperty.getOwner().getName()
				+ "`";
	}
}
