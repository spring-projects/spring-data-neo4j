/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.springframework.core.annotation.AliasFor;

/**
 * The annotation to configure the mapping from a node with a given set of labels to a class and vice versa.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@org.springframework.data.annotation.Persistent
@API(status = API.Status.STABLE, since = "1.0")
public @interface Node {

	/**
	 * @return See {@link #labels()}.
	 */
	@AliasFor("labels")
	String[] value() default {};

	/**
	 * @return The labels to identify a node with that is supposed to be mapped to the class annotated with {@link Node @Node}.
	 */
	@AliasFor("value")
	String[] labels() default {};
}
