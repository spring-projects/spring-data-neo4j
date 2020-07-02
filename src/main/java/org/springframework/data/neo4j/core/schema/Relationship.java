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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to configure mappings of relationship.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "1.0")
public @interface Relationship {

	/**
	 * Enumeration of the direction a relationship can take.
	 * @since 1.0
	 */
	enum Direction {

		/**
		 * Describes an outgoing relationship.
		 */
		OUTGOING,

		/**
		 * Describes an incoming relationship.
		 */
		INCOMING
	}

	/**
	 * @return See {@link #type()}.
	 */
	@AliasFor("type")
	String value() default "";

	/**
	 * @return The type of the relationship.
	 */
	@AliasFor("value")
	String type() default "";

	/**
	 * If {@code direction} is {@link Direction#OUTGOING}, than the attribute annotated with {@link Relationship} will be
	 * the target node of the relationship and the class containing the annotated attribute will be the start node.
	 * <p>
	 * If {@code direction} is {@link Direction#INCOMING}, than the attribute annotated with {@link Relationship} will be
	 * the start node of the relationship and the class containing the annotated attribute will be the end node.
	 *
	 * @return The direction of the relationship.
	 */
	Direction direction() default Direction.OUTGOING;
}
