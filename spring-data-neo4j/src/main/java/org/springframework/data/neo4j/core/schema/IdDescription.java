/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.schema;

import org.apiguardian.api.API;

/**
 * Description howto generate Ids for entities.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class IdDescription {

	private final Id.Strategy idStrategy;

	private final Class<? extends IdGenerator> idGeneratorClass;

	public IdDescription() {
		this(Id.Strategy.INTERNAL, NoopIdGenerator.class);
	}

	public IdDescription(Id.Strategy idStrategy, Class<? extends IdGenerator> idGeneratorClass) {
		this.idStrategy = idStrategy;
		this.idGeneratorClass = idGeneratorClass;
	}

	public Id.Strategy getIdStrategy() {
		return idStrategy;
	}

	public Class<? extends IdGenerator> getIdGeneratorClass() {
		return idGeneratorClass;
	}

	static class NoopIdGenerator implements IdGenerator {

		@Override
		public Object generateId(Object entity) {
			return null;
		}
	}
}
