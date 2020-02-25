package org.neo4j.doc.springframework.data.docs.repositories.domain_events;

// tag::domain-events[]
import org.junit.jupiter.api.Test;
import org.neo4j.springframework.boot.test.autoconfigure.data.DataNeo4jTest;
import org.springframework.beans.factory.annotation.Autowired;

@DataNeo4jTest
public class DomainEventsTest {

	private final ARepository aRepository;

	@Autowired
	public DomainEventsTest(ARepository aRepository) {
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
}
// end::domain-events[]
