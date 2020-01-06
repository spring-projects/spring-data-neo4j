/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.repository.relcentric;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.relcentric.app.Actor;
import org.springframework.data.neo4j.repository.relcentric.app.Movie;
import org.springframework.data.neo4j.repository.relcentric.app.Role;
import org.springframework.data.neo4j.repository.relcentric.app.RoleRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MinimalRelationshipEntityMappingTest.Config.class)
public class MinimalRelationshipEntityMappingTest {

	@Autowired RoleRepository repository;

	@Autowired private TransactionTemplate transactionTemplate;

	@Autowired SessionFactory sessionFactory;

	@Test // DATAGRAPH-1275, OGM-GH-607
	public void verifyUpdateOfRelationshipEntity() {

		long id = transactionTemplate.execute(status -> {
			Actor actor = new Actor("A1");
			Movie movie = new Movie("M1");
			Role role = new Role("R1", actor, movie);
			return repository.save(role).getId();
		});

		Iterable<Map<String, Object>> results = sessionFactory.openSession()
				.query("MATCH (m:Movie) <- [:ACTS_IN] - (:Actor {name: 'A1'}) RETURN collect(m.name) as titles",
						Collections.emptyMap()).queryResults();
		assertThat(results).hasSize(1);
		assertThat(results).first()
				.satisfies(m -> assertThat((String[]) m.get("titles")).containsOnly("M1"));

		transactionTemplate.execute(status -> {
			Role role = repository.findById(id).get();
			role.setMovie(new Movie("M2"));
			return repository.save(role).getId();
		});

		results = sessionFactory.openSession()
				.query("MATCH (m:Movie) <- [:ACTS_IN] - (:Actor {name: 'A1'}) RETURN collect(m.name) as titles",
						Collections.emptyMap()).queryResults();
		assertThat(results).hasSize(1);
		assertThat(results).first()
				.satisfies(m -> assertThat((String[]) m.get("titles")).containsOnly("M2"));
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.repository.relcentric.app")
	static class Config {
	}
}
