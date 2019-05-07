/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Annotation to configure assigment of ids.
 *
 * @author Michael J. Simons
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@org.springframework.data.annotation.Id
@API(status = API.Status.STABLE, since = "1.0")
public @interface Id {

	enum Strategy {

		/**
		 * The default, use Neo4js internal ids.
		 */
		INTERNAL,

		/**
		 * Use assigned values.
		 */
		ASSIGNED,

		/**
		 * Use generated values, generator class is required.
		 */
		GENERATED
	}

	Strategy strategy() default Strategy.INTERNAL;

	Class<? extends IdGenerator> generator() default NoopIdGenerator.class;

	// Needed to make the generator attribute defaultable (null is not a constant)
	class NoopIdGenerator implements IdGenerator {

		@Override
		public Object generateId(Object entity) {
			return null;
		}
	}
}
