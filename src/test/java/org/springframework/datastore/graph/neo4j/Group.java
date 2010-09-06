package org.springframework.datastore.graph.neo4j;

import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.api.GraphEntityRelationship;

import java.util.Collection;

@GraphEntity(fullIndex = true)
public class Group {

	@GraphEntityRelationship(type = "persons", direction = Direction.OUTGOING, elementClass = Person.class)
	private Collection<Person> persons;
	
	@GraphEntityRelationship(type = "persons", elementClass = Person.class)
	private Iterable<Person> readOnlyPersons;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPersons(Collection<Person> persons) {
		this.persons = persons;
	}

	public void addPerson(Person person) {
		persons.add(person);
	}

	public Collection<Person> getPersons() {
		return persons;
	}
	
	public Iterable<Person> getReadOnlyPersons() {
		return readOnlyPersons;
	}

	public void setReadOnlyPersons(Iterable<Person> p) {
		readOnlyPersons = p;
	}
	
}
