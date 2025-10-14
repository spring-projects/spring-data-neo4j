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

/**
 * Annotation for configuring generated values for entity IDs in FalkorDB. This annotation
 * can be used with different ID generation strategies.
 *
 * <p>
 * For internally generated IDs (FalkorDB's internal ID mechanism): <pre>
 * &#64;Id &#64;GeneratedValue
 * private Long id;
 * </pre>
 *
 * <p>
 * For custom ID generators: <pre>
 * &#64;Id &#64;GeneratedValue(UUIDStringGenerator.class)
 * private String uuid;
 * </pre>
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
public @interface GeneratedValue {

	/**
	 * The class of the ID generator to use. If not specified, the framework will use
	 * FalkorDB's internal ID generation for Long/long types.
	 * @return the generator class
	 */
	Class<? extends IdGenerator<?>> generatorClass() default InternalIdGenerator.class;

	/**
	 * Optional reference name for the generator instance.
	 * @return the generator reference
	 */
	String generatorRef() default "";

}
