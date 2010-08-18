package org.springframework.persistence.test;

import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.Graph;

import java.util.Collection;

@Graph.Entity
public class Group {

	@Graph.Entity.Relationship(type = "persons", direction = Direction.OUTGOING, elementClass = Person.class)
	private Collection<Person> persons;

	public void setPersons(Collection<Person> persons) {
		this.persons = persons;
	}

	public void addPerson(Person person) {
		persons.add(person);
	}

	public Collection<Person> getPersons() {
		return persons;
	}
	
}
