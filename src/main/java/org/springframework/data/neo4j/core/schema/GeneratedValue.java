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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import org.apiguardian.api.API;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates a generated id. Ids can be generated internally. by the database itself or by an external generator. This
 * annotation defaults to the internally generated ids.
 * <p>
 * An internal id has no corresponding property on a node. It can only be retrieved via the built-in Cypher function
 * {@code id()}.
 * <p>
 * To use an external id generator, specify on the
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "6.0")
public @interface GeneratedValue {

	/**
	 * @return The generator to use.
	 * @see #generatorClass()
	 */
	@AliasFor("generatorClass")
	Class<? extends IdGenerator<?>> value() default GeneratedValue.InternalIdGenerator.class;

	/**
	 * @return The generator to use. Defaults to {@link InternalIdGenerator}, which indicates database generated values.
	 */
	@AliasFor("value")
	Class<? extends IdGenerator<?>> generatorClass() default GeneratedValue.InternalIdGenerator.class;

	/**
	 * @return An optional reference to a bean to be used as ID generator.
	 */
	String generatorRef() default "";

	/**
	 * This {@link IdGenerator} does nothing. It is used for relying on the internal, database-side created id.
	 */
	final class InternalIdGenerator implements IdGenerator<Void> {

		@Override
		public Void generateId(String primaryLabel, Object entity) {
			return null;
		}
	}

	/**
	 * This generator is automatically applied when a field of type {@link java.util.UUID} is annotated with
	 * {@link Id @Id} and {@link GeneratedValue @GeneratedValue}.
	 *
	 */
	final class UUIDGenerator implements IdGenerator<UUID> {

		@Override
		public UUID generateId(String primaryLabel, Object entity) {
			return UUID.randomUUID();
		}
	}
}
