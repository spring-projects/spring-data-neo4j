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
 * Annotation to configure the mapping of a FalkorDB property to a field.
 *
 * @author Michael J. Simons
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "1.0")
public @interface Property {

	/**
	 * Returns the name of the property.
	 * @return See {@link #name()}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * Returns the name of the property.
	 * @return The name of the property in the FalkorDB graph.
	 */
	@AliasFor("value")
	String name() default "";

}
