package org.springframework.data.neo4j.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.model.Person;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@Transactional
public class RedeclaringRepositoryMethodsTests extends AbstractEntityBasedGraphRepositoryTests {

	@Autowired RedeclaringRepositoryMethodsRepository repository;

	/**
	 * @see DATAGRAPH-392
	 */
	@Test
	public void adjustedWellKnownPagedFindAllMethodShouldReturnOnlyTheUserWithFirstnameOliver() {

		Person olli = repository.save(new Person("Oliver", 30));
		Person tom = repository.save(new Person("Thomas", 30));

		Page<Person> page = repository.findAll(new PageRequest(0, 2));

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getContent().get(0).getName(), is(olli.getName()));
	}

	/**
	 * @see DATAGRAPH-392
	 */
	@Test
	public void adjustedWllKnownFindAllMethodShouldReturnAnEmptyList() {

		Person olli = repository.save(new Person("Oliver", 30));
		Person tom = repository.save(new Person("Thomas", 30));

		EndResult<Person> result = repository.findAll();

		assertThat(result.iterator().hasNext(), is(false));
	}

}
