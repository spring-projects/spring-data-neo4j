/*
 * Copyright 2011-2020 the original author or authors.
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

// tag::domain-events[]

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Example entity for domain events.
 */
@Node
public class AnAggregateRoot {

	@Id private final String name;

	private String someOtherValue;

	@Transient // <1>
	private final Collection<SomeEvent> events = new ArrayList<>();

	public AnAggregateRoot(String name) {
		this.name = name;
	}
	// end::domain-events[]

	public String getName() {
		return name;
	}

	public String getSomeOtherValue() {
		return someOtherValue;
	}
	// tag::domain-events[]

	public void setSomeOtherValue(String someOtherValue) {
		this.events.add(new SomeEvent(this, this.someOtherValue, someOtherValue)); // <2>
		this.someOtherValue = someOtherValue;
	}

	@DomainEvents
	// <3>
	Collection<SomeEvent> domainEvents() {
		return Collections.unmodifiableCollection(events);
	}

	@AfterDomainEventPublication
	// <4>
	void callbackMethod() {
		this.events.clear();
	}
}
// end::domain-events[]
