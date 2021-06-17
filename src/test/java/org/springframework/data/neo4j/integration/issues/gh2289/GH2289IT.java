/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2289;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2289IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@RepeatedTest(23)
	void testNewRelation(@Autowired SkuRepository skuRepo) {
		Sku a = skuRepo.save(new Sku(0L, "A"));
		Sku b = skuRepo.save(new Sku(1L, "B"));
		Sku c = skuRepo.save(new Sku(2L, "C"));
		Sku d = skuRepo.save(new Sku(3L, "D"));

		a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);
		a = skuRepo.save(a);

		Assertions.assertThat(a.getRangeRelationsOut()).hasSize(3);
		b = skuRepo.findById(b.getId()).get();
		Assertions.assertThat(b.getRangeRelationsIn()).hasSize(1);

		b.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		b = skuRepo.save(b);
		Assertions.assertThat(b.getRangeRelationsIn()).hasSize(1);
		Assertions.assertThat(b.getRangeRelationsOut()).hasSize(1);
	}

	@Repository
	public interface SkuRepository extends Neo4jRepository<Sku, Long> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

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
