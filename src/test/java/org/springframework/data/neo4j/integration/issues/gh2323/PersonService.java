/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.issues.gh2323;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * @author Michael J. Simons
 */
@Service
public class PersonService {

	private final PersonRepository personRepository;

	PersonService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	public Person updateRel(String from, List<String> languageNames) {

		List<Knows> knownLanguages = languageNames.stream().map(Language::new)
				.map(language -> new Knows("Some description", language))
				.collect(Collectors.toList());
		return personRepository.updateRel(from, knownLanguages);
	}

	public Optional<Person> updateRel2(String id, List<String> languageNames) {

		Optional<Person> original = personRepository.findById(id);
		if (original.isPresent()) {
			Person person = original.get();
			List<Knows> knownLanguages = languageNames.stream().map(Language::new)
					.map(language -> new Knows("Some description", language))
					.collect(Collectors.toList());
			person.setKnownLanguages(knownLanguages);
			return Optional.of(personRepository.updateRel2(person));
		}

		return original;
	}

	public Optional<Person> updateRel3(String id) {
		Optional<Person> original = personRepository.findById(id);
		if (original.isPresent()) {
			Person person = original.get();
			person.setKnownLanguages(List.of(new Knows("Whatever", new Language("German"))));
			return Optional.of(personRepository.updateRelWith11(person));
		}

		return original;
	}
}
