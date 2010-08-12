package org.springframework.persistence.test;

import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.GraphEntity;
import org.springframework.persistence.graph.Relationship;

import java.util.ArrayList;
import java.util.Collection;

@GraphEntity
public class Group {

	@Relationship(type = "Group.persons", direction = Direction.OUTGOING, elementClass = Person.class)
	private Collection<Person> persons = new ArrayList<Person>();

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
