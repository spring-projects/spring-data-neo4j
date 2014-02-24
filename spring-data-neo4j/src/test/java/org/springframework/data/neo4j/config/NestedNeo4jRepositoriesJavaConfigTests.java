/*
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
package org.springframework.data.neo4j.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.ClassWithNestedRepository.NestedUserRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for JavaConfig configuration with nested repositories.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NestedNeo4jRepositoriesJavaConfigTests {

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = PersonRepository.class, considerNestedRepositories = true)
	static class Config extends Neo4jConfiguration {
        Config() throws ClassNotFoundException {
            setBasePackage(Person.class.getPackage().getName());
        }

        @Bean
		public GraphDatabaseService graphDatabaseService() {
			return new TestGraphDatabaseFactory().newImpermanentDatabase();
		}
	}

	@Autowired NestedUserRepository nestedUserRepository;

	/**
	 * @see DATAGRAPH-399
	 */
	@Test
	public void shouldSupportNestedRepositories() {
		assertThat(nestedUserRepository, is(notNullValue()));
	}
}
