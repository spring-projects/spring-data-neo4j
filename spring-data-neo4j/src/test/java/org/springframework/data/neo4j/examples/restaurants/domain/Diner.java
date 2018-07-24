package org.springframework.data.neo4j.examples.restaurants.domain;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

/**
 * An entity (most likely human) that consumes meals in a Restaurant. Not to be confused with the 50s concept that
 * serves meals.
 */
public class Diner {

	@Id @GeneratedValue private Long id;

	private String firstName;
	private String lastName;

	public Diner() {}

	public Diner(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
