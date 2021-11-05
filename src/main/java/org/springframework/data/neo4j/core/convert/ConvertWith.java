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
package org.springframework.data.neo4j.core.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

/**
 * This annotation can be used to define either custom conversions for single attributes by specifying a custom
 * {@link Neo4jPersistentPropertyConverter} and if needed, a custom factory to create that converter or the annotation
 * can be used to build custom meta-annotated annotations like {@code @org.springframework.data.neo4j.core.support.DateLong}.
 *
 * <p>Custom conversions are applied to both attributes of entities and parameters of repository methods that map to those
 * attributes (which does apply to all derived queries and queries by example but not to string based queries).
 *
 * <p>Converters that have a default constructor don't need a dedicated factory. A dedicated factory will be provided with
 * either this annotation and its values or with the meta annotated annotation, including all configuration
 * available.
 *
 * <p>In case {@link ConvertWith#converterRef()} is set to a non {@literal null} and non-empty value, the mapping context
 * will try to lookup a bean under the given name of type {@link Neo4jPersistentPropertyConverter} in the application context.
 * If no such bean is found an exception will be thrown. This attribute has precedence over {@link ConvertWith#converter()}.
 *
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Abwasser
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD })
@Inherited
@Documented
@API(status = API.Status.STABLE, since = "6.0")
public @interface ConvertWith {

	/**
	 * @return The converter to instantiated for converting attributes to properties and vice versa.
	 */
	Class<? extends Neo4jPersistentPropertyConverter<?>> converter() default UnsetConverter.class;

	/**
	 * @return An alternative to {@link #converter()}, for all the scenarios in which constructing a converter is more effort than a constructor call.
	 */
	Class<? extends Neo4jPersistentPropertyConverterFactory> converterFactory() default DefaultNeo4jPersistentPropertyConverterFactory.class;

	/**
	 * @return An optional reference to a bean to be used as converter, must implement {@link Neo4jPersistentPropertyConverter}.
	 */
	String converterRef() default "";

	/**
	 * Indicates an unset converter.
	 */
	final class UnsetConverter implements Neo4jPersistentPropertyConverter<Object> {

		@Override public Value write(Object source) {
			return Values.NULL;
		}

		@Override public Object read(Value source) {
			return null;
		}
	}
}
