/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to provide Cypher statements that will be used for executing the method. The Cypher statement may contain
 * named parameters as supported by the
 * <a href="https://neo4j.com/docs/driver-manual/1.7/get-started/#driver-get-started-hello-world-example">>Neo4j Java
 * Driver</a>. Those parameters will get bound to the arguments of the annotated method.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@QueryAnnotation
@Documented
@API(status = API.Status.STABLE, since = "6.0")
public @interface Query {

	/**
	 * The custom Cypher query to get executed and mapped back, if any return type is defined.
	 */
	String value() default "";

	/**
	 * The Cypher statement for counting the total number of expected results. Only needed for methods returning pages or slices based on custom queries.
	 */
	String countQuery() default "";

	/**
	 * @return whether the query defined should be executed as count projection.
	 */
	boolean count() default false;

	/**
	 * @return whether the query defined should be executed as exists projection.
	 */
	boolean exists() default false;

	/**
	 * @return whether the query defined should be used to delete nodes or relationships.
	 */
	boolean delete() default false;
}
