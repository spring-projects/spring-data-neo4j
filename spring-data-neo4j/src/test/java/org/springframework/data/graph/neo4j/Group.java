package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.*;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.GraphTraversal;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

import java.lang.reflect.Field;
import java.util.Collection;

@NodeEntity
public class Group {

    @RelatedTo(type = "persons", direction = Direction.OUTGOING, elementClass = Person.class)
    private Collection<Person> persons;

    @RelatedTo(type = "persons", elementClass = Person.class)
    private Iterable<Person> readOnlyPersons;

    @GraphTraversal(traversalBuilder = PeopleTraversalBuilder.class, elementClass = Person.class)
    private Iterable<Person> people;

    @GraphProperty
    @Indexed
    private String name;

    @GraphProperty
    private String unindexedName;

    private String unindexedName2;

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

    public Iterable<Person> getPeople() {
        return people;
    }

    private static class PeopleTraversalBuilder implements FieldTraversalDescriptionBuilder {
        @Override
        public TraversalDescription build(NodeBacked start, Field field) {
            return new TraversalDescriptionImpl()
                    .relationships(DynamicRelationshipType.withName("persons"))
                    .filter(Traversal.returnAllButStartNode());

        }
    }

    public String getUnindexedName() {
        return unindexedName;
    }

    public void setUnindexedName(String unindexedName) {
        this.unindexedName = unindexedName;
    }

    public String getUnindexedName2() {
        return unindexedName2;
    }

    public void setUnindexedName2(String unindexedName2) {
        this.unindexedName2 = unindexedName2;
    }
}
