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
package org.springframework.data.neo4j.documentation.repositories.domain_events;

import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for domain events (and documentation).
 */
@Disabled
@TestPropertySource("foo=The Root")
// tag::domain-events[]
public class DomainEventsTests {

	private final ARepository aRepository;

	@Autowired
	public DomainEventsTests(ARepository aRepository) {
		this.aRepository = aRepository;
		this.aRepository.save(new AnAggregateRoot("The Root"));
	}

	@Test
	void domainEventsShouldWork() {

		this.aRepository.findByName("The Root").ifPresent(anAggregateRoot -> {
			anAggregateRoot.setSomeOtherValue("A new value");
			anAggregateRoot.setSomeOtherValue("Even newer value");
			this.aRepository.save(anAggregateRoot);
		});
	}
	// end::domain-events[]

	@Test
	void customQueryShouldWork() {

		Optional<AnAggregateRoot> optionalAggregate = this.aRepository.findByCustomQuery("The Root");
		assertThat(optionalAggregate).isPresent();

		optionalAggregate = this.aRepository.findByCustomQueryWithSpEL("The ", "Root");
		assertThat(optionalAggregate).isPresent();

		optionalAggregate = this.aRepository.findByCustomQueryWithPropertyPlaceholder();
		assertThat(optionalAggregate).isPresent();
	}

	// tag::domain-events[]

}
// end::domain-events[]
