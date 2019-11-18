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
package org.neo4j.springframework.data.integration.shared;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Persistent;

/**
 * @author Michael J. Simons
 */
@Value
@With
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Persistent
public class ImmutableAuditableThing {

	@Id @GeneratedValue Long id;
	@CreatedDate LocalDateTime createdAt;
	@CreatedBy String createdBy;
	@LastModifiedDate LocalDateTime modifiedAt;
	@LastModifiedBy String modifiedBy;

	String name;

	public ImmutableAuditableThing(String name) {
		this(null, null, null, null, null, name);
	}
}
