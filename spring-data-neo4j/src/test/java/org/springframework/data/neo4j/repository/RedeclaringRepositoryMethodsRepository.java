package org.springframework.data.neo4j.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.model.Person;

/**
 * @author Thomas Darimont
 */
public interface RedeclaringRepositoryMethodsRepository extends GraphRepository<Person> {

	/**
	 * Should not find any persons at all.
	 */
	@Query("START n=node(*) where HAS(n.name) AND n.name='Bubu' return n")
	EndResult<Person> findAll();

	/**
	 * Should only find persons with the name 'Oliver'.
	 * 
	 * @param page
	 * @return
	 */
	@Query("START n=node(*) where HAS(n.name) AND n.name='Oliver' return n")
	Page<Person> findAll(Pageable page);
}
