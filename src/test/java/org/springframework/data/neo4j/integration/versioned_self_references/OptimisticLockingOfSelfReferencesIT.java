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
package org.springframework.data.neo4j.integration.versioned_self_references;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class OptimisticLockingOfSelfReferencesIT extends TestBase {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@ParameterizedTest(name = "{0}")
	@MethodSource("typeAndNewInstanceSupplier")
	<T extends Relatable<T>> void newObjectsSave(Class<T> type, Supplier<T> f) {

		T r1 = f.get();
		T r2 = f.get();
		r1.relate(r2);

		neo4jTemplate.save(r1);

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

		neo4jTemplate.saveAll(Collections.singletonList(r1));

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

		neo4jTemplate.saveAll(Arrays.asList(r1, r2));

		assertDatabase(0L, type, r1);
		assertDatabase(0L, type, r2);
		assertLoadingViaSDN(type, r1.getId(), r2.getId());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSave(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		T r1 = neo4jTemplate.findById(id1, type).get();
		T r2 = neo4jTemplate.findById(id2, type).get();

		r1.relate(r2);

		neo4jTemplate.save(r1);

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSaveAllOne(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		T r1 = neo4jTemplate.findById(id1, type).get();
		T r2 = neo4jTemplate.findById(id2, type).get();

		r1.relate(r2);

		neo4jTemplate.saveAll(Collections.singletonList(r1));

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}") // GH-2355
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsSaveAll(Class<T> type) {

		Long id1 = createInstance(type);
		Long id2 = createInstance(type);

		T r1 = neo4jTemplate.findById(id1, type).get();
		T r2 = neo4jTemplate.findById(id2, type).get();

		r1.relate(r2);

		neo4jTemplate.saveAll(Arrays.asList(r1, r2));

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, id1, id2);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSave(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		T r1 = neo4jTemplate.findById(ids[0], type).get();
		T r2 = neo4jTemplate.findById(ids[1], type).get();

		r1.relate(r2);

		neo4jTemplate.save(r1);

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSaveAllOne(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		T r1 = neo4jTemplate.findById(ids[0], type).get();
		T r2 = neo4jTemplate.findById(ids[1], type).get();

		r1.relate(r2);

		neo4jTemplate.saveAll(Collections.singletonList(r1));

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	@ParameterizedTest(name = "{0}") // GH-2355
	@MethodSource("typesForExistingInstanceSupplier")
	<T extends Relatable<T>> void existingObjectsWithRelationsSaveAll(Class<T> type) {

		long[] ids = createRelatedInstances(type);

		T r1 = neo4jTemplate.findById(ids[0], type).get();
		T r2 = neo4jTemplate.findById(ids[1], type).get();

		r1.relate(r2);

		neo4jTemplate.saveAll(Arrays.asList(r1, r2));

		assertDatabase(1L, type, r1);
		assertDatabase(1L, type, r2);
		assertLoadingViaSDN(type, ids[0], ids[1]);
	}

	private <T extends Relatable<T>> void assertLoadingViaSDN(Class<T> type, Long... ids) {

		for (Long id : ids) {
			assertThat(neo4jTemplate.findById(id, type)).isPresent();
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
