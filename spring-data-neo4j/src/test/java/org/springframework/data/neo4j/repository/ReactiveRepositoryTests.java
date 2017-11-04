/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.domain.sample.SampleEntity;
import org.springframework.data.neo4j.repositories.repo.reactive.ReactiveSampleEntityRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.TransactionalRepositoryTests;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author lilit gabrielyan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ReactiveRepositoryTests.Config.class)
@Transactional
public class ReactiveRepositoryTests {

	@Autowired Session session;

	@Autowired private ReactiveSampleEntityRepository repository;

	private SampleEntity sampleEntity;

	@Before
	public void setUp() {
		session.deleteAll(SampleEntity.class);
		sampleEntity = new SampleEntity("foo", "bar");
		session.save(sampleEntity);
	}

	@Test
	public void testExistsById() throws Exception {
		StepVerifier.create(repository.existsById(sampleEntity.getId())).expectNext(true).verifyComplete();
	}

	@Test
	public void testExistsByIdPublisher() throws Exception {
		StepVerifier.create(repository.existsById(Mono.just(sampleEntity.getId()))).expectNext(true).verifyComplete();
	}

	@Test
	public void testCount() throws Exception {

		StepVerifier.create(repository.count()).expectNext(1L).verifyComplete();
	}

	@Test
	public void testDeleteById() throws Exception {

		StepVerifier.create(repository.deleteById(sampleEntity.getId())).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testDeleteByPublisher() throws Exception {

		StepVerifier.create(repository.deleteById(Flux.just(sampleEntity.getId()))).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testFindAll() throws Exception {

		StepVerifier.create(repository.findAll()).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindAllByIdIterable() throws Exception {

		StepVerifier.create(repository.findAllById(Collections.singleton(sampleEntity.getId()))).expectNext(sampleEntity)
				.verifyComplete();
	}

	@Test
	public void testFindAllByIdIterableAndSort() throws Exception {

		StepVerifier.create(repository.findAllById(Collections.singleton(sampleEntity.getId()),
				Sort.by(new Sort.Order(Sort.Direction.ASC, "id")))).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindAllByIdIterableAndSortDepth() throws Exception {

		StepVerifier.create(repository.findAllById(Collections.singleton(sampleEntity.getId()),
				Sort.by(new Sort.Order(Sort.Direction.ASC, "id")), 1)).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindAllByIdPublisher() throws Exception {
		StepVerifier.create(repository.findAllById(Flux.just(sampleEntity.getId()))).expectNext(sampleEntity)
				.verifyComplete();
	}

	@Test
	public void findAllWithSort() {

		StepVerifier.create(repository.findAll(Sort.by(new Sort.Order(Sort.Direction.ASC, "id")))) //
				.expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void findAllWithSortDepth() {

		StepVerifier.create(repository.findAll(Sort.by(new Sort.Order(Sort.Direction.ASC, "id")), 1)) //
				.expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindById() throws Exception {

		StepVerifier.create(repository.findById(sampleEntity.getId())).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindByIdPublisher() throws Exception {

		StepVerifier.create(repository.findById(Flux.just(sampleEntity.getId()))).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testFindByIdDepth() throws Exception {

		StepVerifier.create(repository.findById(sampleEntity.getId(), 1)).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testSave() throws Exception {

		SampleEntity newEntity = new SampleEntity("baz", "qux");

		StepVerifier.create(repository.save(newEntity)).expectNext(newEntity).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 2);
	}

	@Test
	public void testSaveWithDepth() throws Exception {
		SampleEntity newEntity = new SampleEntity("baz", "qux");

		StepVerifier.create(repository.save(newEntity, 1)).expectNext(newEntity).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 2);

	}

	@Test
	public void testSaveAll() throws Exception {
		SampleEntity e1 = new SampleEntity("baz", "qux");
		SampleEntity e2 = new SampleEntity("qsd", "wxc");

		StepVerifier.create(repository.saveAll(Arrays.asList(e1, e2))).expectNextCount(2).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 3);
	}

	@Test
	public void testSaveAllPublisher() throws Exception {
		SampleEntity e1 = new SampleEntity("baz", "qux");
		SampleEntity e2 = new SampleEntity("qsd", "wxc");

		StepVerifier.create(repository.saveAll(Flux.just(e1, e2))).expectNextCount(2).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 3);
	}

	@Test
	public void testDelete() throws Exception {

		StepVerifier.create(repository.delete(sampleEntity)).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testDeleteAllIterable() throws Exception {

		StepVerifier.create(repository.deleteAll(Collections.singleton(sampleEntity))).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testDeleteAllPublisher() throws Exception {

		StepVerifier.create(repository.deleteAll(Flux.just(sampleEntity))).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testDeleteAll() throws Exception {

		StepVerifier.create(repository.deleteAll()).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	// @Test
	// public void testFindPageable() throws Exception {
	// StepVerifier.create(repository.findAll(PageRequest.of(0,1)))
	// .expectNextMatches(sampleEntities ->
	// sampleEntities.getContent().get(0).equals(sampleEntity)
	// ).verifyComplete();
	//
	// }

	@Test
	public void testCustomQuery() throws Exception {

		StepVerifier.create(repository.getAllByQuery()).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testQueryWithArgument() throws Exception {

		StepVerifier.create(repository.findByFirst("foo")).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testNamedQuery() throws Exception {

		StepVerifier.create(repository.findByQueryWithoutParameter()).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testNamedQueryWithParameter() throws Exception {

		StepVerifier.create(repository.findByQueryWithParameter("foo")).expectNext(sampleEntity).verifyComplete();
	}

	@Test
	public void testDeleteByFirstName() throws Exception {

		StepVerifier.create(repository.removeByFirst("foo")).verifyComplete();

		assertThat("", session.countEntitiesOfType(SampleEntity.class) == 0);
	}

	@Test
	public void testCountByField() throws Exception {

		StepVerifier.create(repository.countByFirst("foo")).expectNext(1L).verifyComplete();
	}

	@Test
	public void shouldFindByObservableOfFirstIn() {

		StepVerifier.create(repository.findByFirstIn(Observable.just("foo"))).expectNextCount(1).verifyComplete();
	}

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.repositories.repo.reactive" })
	@EnableReactiveNeo4jRepositories(basePackages = "org.springframework.data.neo4j.repositories.repo.reactive")
	@EnableTransactionManagement
	public static class Config {

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(
					new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			SessionFactory sessionFactory = null;
			try {
				this.getClass().getClassLoader().loadClass("org.neo4j.ogm.testutil.MultiDriverTestClass");
				org.neo4j.ogm.config.Configuration configuration = MultiDriverTestClass.getBaseConfiguration().build();
				sessionFactory = new SessionFactory(configuration, "org.springframework.data.neo4j.domain.sample");

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return sessionFactory;
		}
	}
}
