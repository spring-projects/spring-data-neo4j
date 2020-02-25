package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

// tag::domain-events[]
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

@Node
public class AnAggregateRoot {

	@Id
	private final String name;

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

	@DomainEvents // <3>
	Collection<SomeEvent> domainEvents() {
		return Collections.unmodifiableCollection(events);
	}

	@AfterDomainEventPublication // <4>
	void callbackMethod() {
		this.events.clear();
	}
}
// end::domain-events[]
