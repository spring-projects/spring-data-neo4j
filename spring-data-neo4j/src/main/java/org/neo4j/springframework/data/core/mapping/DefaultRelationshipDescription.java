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
package org.neo4j.springframework.data.core.mapping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.neo4j.springframework.data.core.schema.RelationshipDescription;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode(of = { "type", "target" })
class DefaultRelationshipDescription implements RelationshipDescription {

	private final String type;

	private final String target;

	/**
	 * If this is set to true, then the type name here is just a placeholder and the actual types shall be retrieved
	 * from the map key.
	 */
	private final boolean dynamic;
}
