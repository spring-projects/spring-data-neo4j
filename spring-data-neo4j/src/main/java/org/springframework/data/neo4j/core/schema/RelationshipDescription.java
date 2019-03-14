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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.apiguardian.api.API;

/**
 * Description of a relationship. Those descriptions always describe outgoing relationships. The inverse direction
 * is maybe defined on the {@link NodeDescription} reachable in the {@link Schema} via it's primary label defined by
 * {@link #target}.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "1.0")
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(of = { "type", "target" })
public final class RelationshipDescription {

	private final String type;

	private final String target;
}
