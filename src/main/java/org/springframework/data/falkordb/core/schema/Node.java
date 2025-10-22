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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

/**
 * The annotation to configure the mapping from a FalkorDB node with a given set of labels
 * to a class and vice versa.
 *
 * @author Michael J. Simons
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Persistent
@API(status = API.Status.STABLE, since = "1.0")
public @interface Node {

	/**
	 * Returns all labels that constitutes this node.
	 * @return See {@link #labels()}.
	 */
	@AliasFor("labels")
	String[] value() default {};

	/**
	 * Returns all labels that constitutes this node.
	 * @return the labels to identify a node with that is supposed to be mapped to the
	 * class annotated with {@link Node @Node}. The first label will be the primary label
	 * if {@link #primaryLabel()} was not set explicitly.
	 */
	@AliasFor("value")
	String[] labels() default {};

	/**
	 * Returns the primary label for this node.
	 * @return The explicit primary label to identify a node.
	 */
	String primaryLabel() default "";

	/**
	 * Defines aggregate boundaries for the entity.
	 * @return Classes that define the aggregate boundary
	 */
	Class<?>[] aggregateBoundary() default {};

}
