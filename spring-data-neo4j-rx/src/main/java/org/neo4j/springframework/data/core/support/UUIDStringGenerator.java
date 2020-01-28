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
package org.neo4j.springframework.data.core.support;

import java.util.UUID;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.schema.IdGenerator;

/**
 * A generator providing UUIDs.
 *
 * @author Michael J. Simons
 * @soundtrack Various - Kung Fury (Original Motion Picture Soundtrack)
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class UUIDStringGenerator implements IdGenerator<String> {

	@Override
	public String generateId(String primaryLabel, Object entity) {
		return UUID.randomUUID().toString();
	}
}
