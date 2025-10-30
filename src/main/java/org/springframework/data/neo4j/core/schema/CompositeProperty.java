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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

/**
 * This annotation indicates a
 * {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty persistent
 * property} that is composed of multiple properties on a node or relationship. The
 * properties must share a common prefix. SDN defaults to the name of the field declared
 * on the {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity
 * persistent entity}.
 * <p>
 * This annotation is mainly to be used on properties of type {@link Map Map&lt;String,
 * Object&gt;}. All values in the map are subject to conversions by other registered
 * converters. <b>Nested maps are not supported.</b>
 * <p>
 * This annotation is the pendant to Neo4j-OGMs
 * {@literal org.neo4j.ogm.annotation.Properties}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
@ConvertWith(converterFactory = CompositePropertyConverterFactory.class)
@API(status = API.Status.STABLE, since = "6.0")
public @interface CompositeProperty {

	/**
	 * Defines a dedicated converter for this field.
	 * @return A converter that allows to store arbitrary objects as decomposed maps on
	 * nodes and relationships. The default converter allows only maps as composite
	 * properties.
	 */
	@AliasFor(annotation = ConvertWith.class, value = "converter")
	Class<? extends Neo4jPersistentPropertyToMapConverter> converter() default CompositeProperty.DefaultToMapConverter.class;

	@AliasFor(annotation = ConvertWith.class, value = "converterRef")
	String converterRef() default "";

	/**
	 * Allows to specify the prefix for the map properties. The default empty value
	 * instructs SDN to use the field name of the annotated property.
	 * @return The prefix used for storing the properties in the graph on the node or
	 * relationship
	 */
	String prefix() default "";

	/**
	 * Allows to specify the delimiter between prefix and map value on the properties of
	 * the node or relationship in the graph. Defaults to {@literal .}.
	 * @return Delimiter to use in the stored property names
	 */
	String delimiter() default ".";

	/**
	 * This attribute allows for configuring a transformation that is applied to the maps
	 * keys. {@link Phase#WRITE} is applied before writing the map, {@link Phase#READ} is
	 * applied on write.
	 * @return A transformation to be used on enum keys.
	 */
	Class<? extends BiFunction<Phase, String, String>> transformKeysWith() default NoopTransformation.class;

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
	 * The default implementation, passing map properties through as they are on the way
	 * to the graph and possibly applying a post processor on the way out of the graph.
	 *
	 * @param <K> the type of the keys
	 */
	final class DefaultToMapConverter<K> implements Neo4jPersistentPropertyToMapConverter<K, Map<K, Object>> {

		/**
		 * A post processor of the map that is eventually be stored in the entity. In case
		 * a user wishes for entities with immutable collection, that would be the place
		 * to configure it.
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
					(k, v) -> decomposed.put(k, conversionService.writeValue(v, this.typeInformationForValues, null)));
			return decomposed;
		}

		@Override
		public Map<K, Object> compose(Map<K, Value> source, Neo4jConversionService conversionService) {
			Map<K, Object> composed = new HashMap<>(source.size());
			source.forEach(
					(k, v) -> composed.put(k, conversionService.readValue(v, this.typeInformationForValues, null)));
			return this.mapPostProcessor.apply(composed);
		}

	}

}
