/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.examples.spring_boot.web;

import java.util.Collections;

import org.neo4j.springframework.data.examples.spring_boot.domain.PersonEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

/**
 * A standard web controller showcasing the Spring Web MVC Integration
 *
 * @author Michael J. Simons
 */
@Controller
@RequestMapping("/people")
public class PeopleController {

	private final PersonRepository personRepository;

	public PeopleController(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@GetMapping("/{name}")
	ModelAndView showPersonForm(@PathVariable("name") PersonEntity person) {

		return new ModelAndView("personForm", Collections.singletonMap("person", person));
	}

	@PostMapping
	String updatePerson(PersonEntity updatedPerson) {

		this.personRepository.findByName(updatedPerson.getName()).ifPresent(p -> {
			p.setBorn(updatedPerson.getBorn());
			this.personRepository.save(p);
		});

		return "redirect:" + MvcUriComponentsBuilder.fromMethodName(
			this.getClass(), "showPersonForm", updatedPerson).build();
	}
}
