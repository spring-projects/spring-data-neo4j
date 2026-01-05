/*
 * Copyright 2011-present the original author or authors.
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
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "6.0")
public @interface Relationship {

	/**
	 * Returns the type of the relationship.
	 * @return See {@link #type()}.
	 */
	@AliasFor("type")
	String value() default "";

	/**
	 * Returns the type of the relationship.
	 * @return The type of the relationship.
	 */
	@AliasFor("value")
	String type() default "";

	/**
	 * If {@code direction} is {@link Direction#OUTGOING}, than the attribute annotated
	 * with {@link Relationship} will be the target node of the relationship and the class
	 * containing the annotated attribute will be the start node.
	 * <p>
	 * If {@code direction} is {@link Direction#INCOMING}, than the attribute annotated
	 * with {@link Relationship} will be the start node of the relationship and the class
	 * containing the annotated attribute will be the end node.
	 * @return The direction of the relationship.
	 */
	Direction direction() default Direction.OUTGOING;

	/**
	 * Set this attribute to {@literal false} if you don't want updates on an aggregate
	 * root to be cascaded to related objects. Be aware that in this case you are
	 * responsible to manually save the related objects and that you might end up with a
	 * local object graph that is not in sync with the actual graph.
	 * @return whether updates to the owning instance should be cascaded to the related
	 * objects
	 */
	boolean cascadeUpdates() default true;

	/**
	 * Enumeration of the direction a relationship can take.
	 *
	 * @since 6.0
	 */
	enum Direction {

		/**
		 * Describes an outgoing relationship.
		 */
		OUTGOING,

		/**
		 * Describes an incoming relationship.
		 */
		INCOMING;

		public Direction opposite() {
			return (this != OUTGOING) ? OUTGOING : INCOMING;
		}

	}

}
