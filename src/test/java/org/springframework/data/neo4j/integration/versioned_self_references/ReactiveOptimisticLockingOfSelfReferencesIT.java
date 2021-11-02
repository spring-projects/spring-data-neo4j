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
package org.springframework.data.neo4j.integration.versioned_self_references;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class ReactiveOptimisticLockingOfSelfReferencesIT extends TestBase {

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@ParameterizedTest(name = "{0}")
	@MethodSource("typeAndNewInstanceSupplier")
	<T extends Relatable<T>> void newObjectsSave(Class<T> type, Supplier<T> f) {

		T r1 = f.get();
		T r2 = f.get();
		r1.relate(r2);

		neo4jTemplate.save(r1)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(0L, type, r1);
		assertDatabase(0L, type, r2);
		assertLoadingViaSDN(type, r1.getId(), r2.getId());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typeAndNewInstanceSupplier")
	<T extends Relatable<T>> void newObjectsSaveAllOne(Class<T> type, Supplier<T> f) {

		T r1 = f.get();
		T r2 = f.get();
		r1.relate(r2);

		neo4jTemplate.saveAll(Collections.singletonList(r1))
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(0L, type, r1);
		assertDatabase(0L, type, r2);
		assertLoadingViaSDN(type, r1.getId(), r2.getId());
	}

	@ParameterizedTest(name = "{0}") // GH-2355
	@MethodSource("typeAndNewInstanceSupplier")
	<T extends Relatable<T>> void newObjectsSaveAll(Class<T> type, Supplier<T> f) {

		T r1 = f.get();
		T r2 = f.get();
		r1.relate(r2);

		neo4jTemplate.saveAll(Arrays.asList(r1, r2))
				.as(StepVerifier::create).expectNextCount(2L).verifyComplete();

		assertDatabase(0L, type, r1);
		assertDatabase(0L, type, r2);
		assertLoadingViaSDN(type, r1.getId(), r2.getId());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSave(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(id1, type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(id2, type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.save(r1.get())
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSaveAllOne(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(id1, type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(id2, type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.saveAll(Collections.singletonList(r1).stream().map(AtomicReference::get).collect(Collectors.toList()))
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}") // GH-2355
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSaveAll(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(id1, type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(id2, type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.saveAll(Arrays.asList(r1, r2).stream().map(AtomicReference::get).collect(Collectors.toList()))
				.as(StepVerifier::create).expectNextCount(2L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSave(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(ids[0], type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(ids[1], type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.save(r1.get())
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSaveAllOne(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(ids[0], type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(ids[1], type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.saveAll(Arrays.asList(r1, r2).stream().map(AtomicReference::get).collect(Collectors.toList()))
				.as(StepVerifier::create).expectNextCount(2L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	@ParameterizedTest(name = "{0}") // GH-2355
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSaveAll(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		AtomicReference<T> r1 = new AtomicReference<>();
		AtomicReference<T> r2 = new AtomicReference<>();
		neo4jTemplate.findById(ids[0], type)
				.doOnNext(r1::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		neo4jTemplate.findById(ids[1], type)
				.doOnNext(r2::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		r1.get().relate(r2.get());

		neo4jTemplate.saveAll(Collections.singletonList(r1).stream().map(AtomicReference::get).collect(Collectors.toList()))
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		assertDatabase(1L, type, r1.get());
		assertDatabase(1L, type, r2.get());
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	@Test
	void creatingLoadingAndModifyingARingShouldWork() {
		int ringSize = 5;

		AtomicReference<VersionedExternalIdWithEquals> ref = new AtomicReference<>();

		VersionedExternalIdWithEquals start = createRing(ringSize);
		neo4jTemplate.save(start)
				.doOnNext(ref::set)
				.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		start = ref.get();

		neo4jTemplate.findById(start.getId(), VersionedExternalIdWithEquals.class)
				.as(StepVerifier::create)
				.expectNextMatches(root -> {
					int traversedObjects = traverseRing(root, next -> {
						assertThat(next.getRelatedObjects()).hasSize(2);
						assertThat(next.getVersion()).isEqualTo(0L);
					});
					assertThat(traversedObjects).isEqualTo(ringSize);
					return true;
				})
				.verifyComplete();

		String newName = "A new beginning";
		start.setName(newName);
		neo4jTemplate.saveAs(start, NameOnly.class).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		neo4jTemplate.findById(start.getId(), VersionedExternalIdWithEquals.class)
				.as(StepVerifier::create)
				.expectNextMatches(root -> {
					assertThat(root.getName()).isEqualTo(newName);
					assertThat(root.getVersion()).isEqualTo(1L);

					int traversedObjects = traverseRing(root, next -> {
						assertThat(next.getRelatedObjects()).hasSize(2);
						assertThat(next.getVersion()).isEqualTo(next.getName().equals(newName) ? 1L : 0L);
					});
					assertThat(traversedObjects).isEqualTo(ringSize);
					return true;
				})
				.verifyComplete();
	}

	private <T extends Relatable<T>> void assertLoadingViaSDN(Class<T> type, Long... ids) {

		for (Long id : ids) {
			neo4jTemplate.findById(id, type).as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
