/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2533;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for projection with dynamic relationships.
 */
@Neo4jIntegrationTest
public class GH2533ReactiveIT {
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	private ReactiveGH2533Repository repository;

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2533
	void projectionWorksForDynamicRelationshipsOnSave() {
		createData()
				.flatMap(rootEntity -> repository.findByIdWithLevelOneLinks(rootEntity.id))
				.flatMap(rootEntity -> neo4jTemplate.saveAs(rootEntity, EntitiesAndProjections.GH2533EntityNodeWithOneLevelLinks.class))
				.flatMap(rootEntity -> neo4jTemplate.findById(rootEntity.getId(), EntitiesAndProjections.GH2533Entity.class))
				.as(StepVerifier::create)
				.assertNext(entity -> {
					assertThat(entity.relationships).isNotEmpty();
					assertThat(entity.relationships.get("has_relationship_with")).isNotEmpty();
					assertThat(entity.relationships.get("has_relationship_with").get(0).target).isNotNull();
					assertThat(entity.relationships.get("has_relationship_with").get(0).target.relationships).isNotEmpty();
				})
				.verifyComplete();
	}

	@Test // GH-2533
	void saveRelatedEntityWithRelationships() {
		createData()
				.flatMap(rootEntity -> repository.findByIdWithLevelOneLinks(rootEntity.id))
				.flatMap(rootEntity -> neo4jTemplate.saveAs(rootEntity, EntitiesAndProjections.GH2533EntityNodeWithOneLevelLinks.class))
				.flatMap(rootEntity -> neo4jTemplate.findById(rootEntity.getId(), EntitiesAndProjections.GH2533Entity.class))
				.as(StepVerifier::create)
				.assertNext(entity -> {
					assertThat(entity.relationships.get("has_relationship_with").get(0).target.name).isEqualTo("n2");
					assertThat(entity.relationships.get("has_relationship_with").get(0).target.relationships.get("has_relationship_with").get(0).target.name).isEqualTo("n3");
				})
				.verifyComplete();
	}

	private Mono<EntitiesAndProjections.GH2533Entity> createData() {
		EntitiesAndProjections.GH2533Entity n1 = new EntitiesAndProjections.GH2533Entity();
		EntitiesAndProjections.GH2533Entity n2 = new EntitiesAndProjections.GH2533Entity();
		EntitiesAndProjections.GH2533Entity n3 = new EntitiesAndProjections.GH2533Entity();

		EntitiesAndProjections.GH2533Relationship r1 = new EntitiesAndProjections.GH2533Relationship();
		EntitiesAndProjections.GH2533Relationship r2 = new EntitiesAndProjections.GH2533Relationship();

		n1.name = "n1";
		n2.name = "n2";
		n3.name = "n3";

		r1.target = n2;
		r2.target = n3;

		n1.relationships = Collections.singletonMap("has_relationship_with", Arrays.asList(r1));
		n2.relationships = Collections.singletonMap("has_relationship_with", Arrays.asList(r2));

		return repository.save(n1);
	}


	interface ReactiveGH2533Repository extends ReactiveNeo4jRepository<EntitiesAndProjections.GH2533Entity, Long> {
		@Query("MATCH p=(n)-[*0..1]->(m) WHERE id(n)=$id RETURN n, collect(relationships(p)), collect(m);")
		Mono<EntitiesAndProjections.GH2533Entity> findByIdWithLevelOneLinks(@Param("id") Long id);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}
	}
}
