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

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Example stub for a Spring Boot application to get included in the documentation.
 */
// @SpringBootApplication
public class DomainEventsApplication {

	// tag::domain-events[]
	@Bean
	ApplicationListener<SomeEvent> someEventListener() {
		return event -> log("someOtherValue changed from '" + event.getOldValue() + "' to '" + event.getNewValue()
				+ "' at " + event.getChangeHappenedAt());
	}
	// end::domain-events[]

	void log(String message) {
	}

}
