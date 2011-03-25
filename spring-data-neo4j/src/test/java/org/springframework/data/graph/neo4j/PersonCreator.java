package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.data.persistence.StateBackedCreator;

/**
 * @author mh
 * @since 25.03.11
 */
public class PersonCreator implements StateBackedCreator<Person,Node> {
    @Override
    public Person create(Node n, Class<Person> c) throws Exception {
        Person person = new Person();
        person.setPersistentState(n);
        return person;
    }
}
