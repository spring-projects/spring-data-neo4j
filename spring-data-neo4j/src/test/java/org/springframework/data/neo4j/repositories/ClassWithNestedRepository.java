package org.springframework.data.neo4j.repositories;

import org.springframework.data.neo4j.model.Person;
import org.springframework.data.repository.Repository;

/**
 * @see DATAGRAPH-399
 * @author Thomas Darimont
 */
public class ClassWithNestedRepository {

	public static interface NestedUserRepository extends Repository<Person, Integer> {}
}
