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
