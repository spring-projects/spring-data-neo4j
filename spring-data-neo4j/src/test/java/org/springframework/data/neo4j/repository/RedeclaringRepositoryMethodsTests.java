/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.RedeclaringRepositoryMethodsRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@Transactional
public class RedeclaringRepositoryMethodsTests extends AbstractEntityBasedGraphRepositoryTests {

	@Autowired
    RedeclaringRepositoryMethodsRepository repository;

	/**
	 * @see DATAGRAPH-392
	 */
	@Test
	public void adjustedWellKnownPagedFindAllMethodShouldReturnOnlyTheUserWithFirstnameOliver() {

		Person ollie = repository.save(new Person("Oliver", 30));
		repository.save(new Person("Thomas", 30));

		Page<Person> page = repository.findAll(new PageRequest(0, 2));

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getContent().get(0).getName(), is(ollie.getName()));
	}

	/**
	 * @see DATAGRAPH-392
	 */
	@Test
	public void adjustedWllKnownFindAllMethodShouldReturnAnEmptyList() {

		repository.save(new Person("Oliver", 30));
		repository.save(new Person("Thomas", 30));

		Result<Person> result = repository.findAll();

		assertThat(result.iterator().hasNext(), is(false));
	}
}
