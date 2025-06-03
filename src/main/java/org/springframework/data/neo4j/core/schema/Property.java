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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

import org.springframework.core.annotation.AliasFor;

/**
 * The annotation to configure the mapping from a property to an attribute and vice versa.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "6.0")
public @interface Property {

	/**
	 * The name of this property in the graph.
	 * @return See {@link #name()}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of this property in the graph.
	 * @return The name of the property in the graph.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * A flag if this property should be treated as read only.
	 * @return Set this attribute to {@literal true} to prevent writing any value of this
	 * property to the graph.
	 */
	boolean readOnly() default false;

}
