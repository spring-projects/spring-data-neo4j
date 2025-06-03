/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.cdi;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * This domain object features a client side generated ID on purpose. It is needed to
 * verify that the callbacks generating those are actually registered correct.
 *
 * @author Michael J. Simons
 */
@Node
class Person {

	private final String name;

	@Id
	@GeneratedValue
	private UUID id;

	@CreatedDate
	private LocalDate createdAt;

	Person(String name) {
		this.name = name;
	}

	UUID getId() {
		return this.id;
	}

	String getName() {
		return this.name;
	}

	LocalDate getCreatedAt() {
		return this.createdAt;
	}

}
