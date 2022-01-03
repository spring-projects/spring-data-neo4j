/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.documentation.repositories.domain_events;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEvent;

/**
 * Some example event.
 */
// tag::domain-events[]
public class SomeEvent extends ApplicationEvent { // <1>

	private final LocalDateTime changeHappenedAt = LocalDateTime.now();

	private final String oldValue;

	private final String newValue;

	public SomeEvent(AnAggregateRoot source, String oldValue, String newValue) {
		super(source);
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	// Getters omitted...
	// end::domain-events[]

	public LocalDateTime getChangeHappenedAt() {
		return changeHappenedAt;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}
	// tag::domain-events[]
}
// end::domain-events[]
