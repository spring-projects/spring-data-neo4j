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
package org.springframework.data.neo4j.integration.issues.gh2289;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveGH2289IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");
			for (int i = 0; i < 4; ++i) {
				transaction.run("CREATE (s:SKU_RO {number: $i, name: $n})",
						Values.parameters("i", i, "n", new String(new char[] { (char) ('A' + i) })));
			}
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@RepeatedTest(23)
	void testNewRelation(@Autowired SkuRepository skuRepo) {

		AtomicLong aId = new AtomicLong();
		AtomicLong bId = new AtomicLong();
		AtomicReference<Sku> cRef = new AtomicReference<>();
		skuRepo.save(new Sku(0L, "A"))
				.zipWith(skuRepo.save(new Sku(1L, "B")))
				.zipWith(skuRepo.save(new Sku(2L, "C")))
				.zipWith(skuRepo.save(new Sku(3L, "D"))).flatMap(t -> {
			Sku a = t.getT1().getT1().getT1();
			Sku b = t.getT1().getT1().getT2();
			Sku c = t.getT1().getT2();
			Sku d = t.getT2();

			bId.set(b.getId());
			cRef.set(c);
			a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
			a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
			a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);
			return skuRepo.save(a);
		}).as(StepVerifier::create)
				.expectNextMatches(a -> {
					aId.set(a.getId()); // side-effects for the win
					return a.getRangeRelationsOut().size() == 3;
				})
				.verifyComplete();

		skuRepo.findById(bId.get())
				.doOnNext(b -> assertThat(b.getRangeRelationsIn()).hasSize(1))
				.flatMap(b -> {
					b.rangeRelationTo(cRef.get(), 1, 1, RelationType.MULTIPLICATIVE);
					return skuRepo.save(b);
				})
				.as(StepVerifier::create)
				.assertNext(b -> {
					assertThat(b.getRangeRelationsIn()).hasSize(1);
					assertThat(b.getRangeRelationsOut()).hasSize(1);
				})
				.verifyComplete();

		skuRepo.findById(aId.get())
				.as(StepVerifier::create)
				.assertNext(a -> {
					assertThat(a.getRangeRelationsOut()).hasSize(3);
					assertThat(a.getRangeRelationsOut()).allSatisfy(r -> {
						int expectedSize = 1;
						if ("C".equals(r.getTargetSku().getName())) {
							expectedSize = 2;
						}
						assertThat(r.getTargetSku().getRangeRelationsIn()).hasSize(expectedSize);
					});
				})
				.verifyComplete();
	}

	@RepeatedTest(5) // GH-2294
	void testNewRelationRo(@Autowired SkuRORepository skuRepo) {

		AtomicLong bId = new AtomicLong();
		AtomicReference<SkuRO> cRef = new AtomicReference<>();
		skuRepo.findOneByName("A")
				.zipWith(skuRepo.findOneByName("B"))
				.zipWith(skuRepo.findOneByName("C"))
				.zipWith(skuRepo.findOneByName("D"))
				.flatMap(t -> {
			SkuRO a = t.getT1().getT1().getT1();
			SkuRO b = t.getT1().getT1().getT2();
			SkuRO c = t.getT1().getT2();
			SkuRO d = t.getT2();

			bId.set(b.getId());
			cRef.set(c);
			a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
			a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
			a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);

			a.setName("a new name");

			return skuRepo.save(a);
		}).as(StepVerifier::create)
				.expectNextMatches(a -> a.getRangeRelationsOut().size() == 3 && "a new name".equals(a.getName()))
				.verifyComplete();

		skuRepo.findOneByName("a new name")
				.as(StepVerifier::create)
				.verifyComplete();

		skuRepo.findOneByName("B")
				.doOnNext(b -> {
					assertThat(b.getRangeRelationsIn()).hasSize(1);
					assertThat(b.getRangeRelationsOut()).hasSizeLessThanOrEqualTo(1);
				})
				.flatMap(b -> {
					b.rangeRelationTo(cRef.get(), 1, 1, RelationType.MULTIPLICATIVE);
					return skuRepo.save(b);
				})
				.as(StepVerifier::create)
				.expectNextMatches(b -> b.getRangeRelationsIn().size() == 1 && b.getRangeRelationsOut().size() == 1)
				.verifyComplete();
	}

	@Repository
	public interface SkuRepository extends ReactiveNeo4jRepository<Sku, Long> {
	}

	@Repository
	public interface SkuRORepository extends ReactiveNeo4jRepository<SkuRO, Long> {

		Mono<SkuRO> findOneByName(String name);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
