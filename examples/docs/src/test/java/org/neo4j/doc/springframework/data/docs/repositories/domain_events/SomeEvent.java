package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

// tag::domain-events[]
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEvent;

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
