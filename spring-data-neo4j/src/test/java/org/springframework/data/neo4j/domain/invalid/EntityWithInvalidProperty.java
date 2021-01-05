/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.domain.invalid;

import java.time.Year;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

/**
 * An entity with invalid properties that should fail serialisation and persistence.
 *
 * @author Michael J. Simons
 * @soundtrack Opeth - Blackwater Park
 */
@NodeEntity
public class EntityWithInvalidProperty {
	private Long id;

	// This property would need a converter.
	@Property(name = "yearOfBirth") private Year year;

	public EntityWithInvalidProperty(Year year) {
		this.year = year;
	}

	public Long getId() {
		return id;
	}

	public Year getYear() {
		return year;
	}
}
