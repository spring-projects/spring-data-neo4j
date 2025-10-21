/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to configure mappings of FalkorDB relationships.
 *
 * @author Michael J. Simons
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "1.0")
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
		INCOMING,

		/**
		 * Describes an undirected relationship.
		 */
		UNDIRECTED;

		public Direction opposite() {
			switch (this) {
				case OUTGOING:
					return INCOMING;
				case INCOMING:
					return OUTGOING;
				case UNDIRECTED:
					return UNDIRECTED;
				default:
					throw new IllegalStateException("Unknown direction: " + this);
			}
		}

	}

}
