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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

/**
 * The annotation to configure the mapping from a node with a given set of labels to a
 * class and vice versa.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Persistent
@API(status = API.Status.STABLE, since = "6.0")
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

}
