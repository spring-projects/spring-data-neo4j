/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import java.util.function.BiFunction;

import org.apiguardian.api.API;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

/**
 * Used to keep the raw result around in case of a DTO based projection so that missing properties can be filled later on.
 *
 * @author Michael J. Simons
 * @soundtrack The Prodigy - Music For The Jilted Generation
 */
@API(status = API.Status.INTERNAL, since = "6.1.2")
public final class EntityInstanceWithSource {

	/**
	 * An instance of the original {@link org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity source entity}
	 */
	private final Object entityInstance;

	/**
	 * The type system used to extract data.
	 */
	private final TypeSystem typeSystem;

	/**
	 * The record from which the source above was hydrated and which might contain top level properties that are eligible to mapping.
	 */
	private final MapAccessor sourceRecord;

	public static BiFunction<TypeSystem, MapAccessor, ?> decorateMappingFunction(BiFunction<TypeSystem, MapAccessor, ?> target) {
		return (t, r) -> new EntityInstanceWithSource(target.apply(t, r), t, r);
	}

	private EntityInstanceWithSource(Object entityInstance, TypeSystem typeSystem, MapAccessor sourceRecord) {

		this.entityInstance = entityInstance;
		this.typeSystem = typeSystem;
		this.sourceRecord = sourceRecord;
	}

	public Object getEntityInstance() {
		return entityInstance;
	}

	public TypeSystem getTypeSystem() {
		return typeSystem;
	}

	public MapAccessor getSourceRecord() {
		return sourceRecord;
	}
}
