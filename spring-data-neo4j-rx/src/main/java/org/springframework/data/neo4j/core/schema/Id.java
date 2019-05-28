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
import java.util.EnumSet;

import org.apiguardian.api.API;

/**
 * Annotation to configure assigment of ids.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@org.springframework.data.annotation.Id
@API(status = API.Status.STABLE, since = "1.0")
public @interface Id {

	/**
	 * Enumerating available strategies for providing ids for new entities.
	 */
	enum Strategy {

		/**
		 * The default, use Neo4js internal ids.
		 */
		INTERNALLY_GENERATED,

		/**
		 * Use assigned values.
		 */
		ASSIGNED,

		/**
		 * Use externally generated values, a generator class is required.
		 */
		EXTERNALLY_GENERATED;

		/**
		 * @return True, if the database generated the ID.
		 */
		public boolean isInternal() {
			return this == INTERNALLY_GENERATED;
		}

		/**
		 * @return True, if the ID is assigned to the entity before the entity hits the database
		 */
		public boolean isExternal() {
			return EnumSet.of(ASSIGNED, EXTERNALLY_GENERATED).contains(this);
		}
	}

	/**
	 * Configure the strategy for generating ids. Some strategies require a {@link #generator()}.
	 *
	 * @return The strategy applied for generating ids for new entities. Defaults to use Neo4j internal database identifiers.
	 */
	Strategy strategy() default Strategy.INTERNALLY_GENERATED;

	/**
	 *
	 * @return The generator that fits the selected {@link #strategy()}. Defaults to a Noop generator.
	 */
	Class<? extends IdGenerator> generator() default NoopIdGenerator.class;

	// Needed to make the generator attribute defaultable (null is not a constant)
	class NoopIdGenerator implements IdGenerator {

		@Override
		public Object generateId(Object entity) {
			return null;
		}
	}
}
