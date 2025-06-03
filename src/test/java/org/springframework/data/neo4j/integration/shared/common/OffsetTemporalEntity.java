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
package org.springframework.data.neo4j.integration.shared.common;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Using some offset temporal types.
 *
 * @author Michael J. Simons
 */
@Node
public class OffsetTemporalEntity {

	@Id
	@GeneratedValue
	private UUID uuid;

	private OffsetDateTime property1;

	private LocalTime property2;

	public OffsetTemporalEntity(OffsetDateTime property1, LocalTime property2) {
		this.property1 = property1;
		this.property2 = property2;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public OffsetDateTime getProperty1() {
		return this.property1;
	}

	public void setProperty1(OffsetDateTime property1) {
		this.property1 = property1;
	}

	public LocalTime getProperty2() {
		return this.property2;
	}

	public void setProperty2(LocalTime property2) {
		this.property2 = property2;
	}

}
