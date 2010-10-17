package org.springframework.datastore.graph.neo4j;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.datastore.graph.annotations.Indexed;
import org.springframework.datastore.graph.annotations.GraphTraversal;
import org.springframework.datastore.graph.annotations.NodeEntity;
import org.springframework.datastore.graph.annotations.RelatedTo;
import org.springframework.datastore.graph.api.*;

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

    @Indexed
    private String name;

    @Indexed(index = false)
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
