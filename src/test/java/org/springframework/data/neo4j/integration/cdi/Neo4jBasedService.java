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
package org.springframework.data.neo4j.integration.cdi;

import javax.inject.Inject;

import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 * @soundtrack Various - TRON Legacy R3conf1gur3d
 */
class Neo4jBasedService {

	final Driver driver;

	final PersonRepository personRepository;

	@Inject
	Neo4jBasedService(Driver driver, PersonRepository personRepository) {
		this.driver = driver;
		this.personRepository = personRepository;
	}
}
