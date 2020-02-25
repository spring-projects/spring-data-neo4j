package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DomainEventsApplication {

	// tag::domain-events[]
	@Bean
	ApplicationListener<SomeEvent> someEventListener() {
		return event -> System.out.println(
			"someOtherValue changed from '" + event.getOldValue() + "' to '" + event.getNewValue() + "' at " + event
				.getChangeHappenedAt());
	}
	// end::domain-events[]
}
