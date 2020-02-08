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
package org.springframework.data.neo4j.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to declare finder queries directly on repository methods.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Gerrit Meier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@QueryAnnotation
@Documented
public @interface Query {

	/**
	 * Defines the Cypher query to be executed when the annotated method is called.
	 */
	String value() default "";

	/**
	 * @return simpler count-query to be executed for @{see Pageable}-support
	 */
	String countQuery() default "";

	/**
	 * Returns whether the defined query should be executed as an exists projection.
	 *
	 * @since 5.2
	 */
	boolean exists() default false;
}
