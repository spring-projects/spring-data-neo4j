/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.cascading;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

@Neo4jIntegrationTest
@Import(CascadingIT.Config.class)
class CascadingIT extends AbstractCascadingTestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	Neo4jTemplate template;

	@CartesianTest
	<T extends Parent> void updatesMustNotCascade(
			@Values(classes = { PUI.class, PUE.class, PVI.class, PVE.class }) Class<T> type,
			@Values(booleans = { true, false }) boolean single)
			throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

		var id = EXISTING_IDS.get(type);
		var instance = this.template.findById(id, type).orElseThrow();

		instance.setName("Updated parent");
		instance.getSingleCUE().setName("Updated single CUE");
		instance.getSingleCUI().setName("Updated single CUI");
		instance.getManyCUI().forEach(cui -> {
			cui.setName(cui.getName() + ".updatedNested1");
			cui.getNested().forEach(nested -> nested.setName(nested + ".updatedNested2"));
		});

		if (single) {
			this.template.save(instance);
		}
		else {
			this.template.saveAll(List.of(instance, type.getDeclaredConstructor(String.class).newInstance("Parent2")));
		}

		// Can't assert on the instance above, as that would ofc be the purposefully
		// modified state
		var reloadedInstance = this.template.findById(id, type).orElseThrow();
		assertThat(reloadedInstance.getName()).isEqualTo("Updated parent");

		assertThat(reloadedInstance.getSingleCUE().getName()).isEqualTo("ParentDB.singleCUE");
		assertThat(reloadedInstance.getSingleCUI().getName()).isEqualTo("ParentDB.singleCUI");
		assertThat(reloadedInstance.getSingleCVE().getVersion()).isZero();
		assertThat(reloadedInstance.getSingleCVI().getVersion()).isZero();
		assertThat(reloadedInstance.getManyCUI()).allMatch(cui -> cui.getName().endsWith(".updatedNested1")
				&& cui.getNested().stream().noneMatch(nested -> nested.getName().endsWith(".updatedNested2")));
		assertThat(reloadedInstance.getManyCVI()).allMatch(cvi -> cvi.getVersion() == 0L);
	}

	@CartesianTest
	<T extends Parent> void newItemsMustBePersistedRegardlessOfCascadeSingleSave(
			@Values(classes = { PUI.class, PUE.class, PVI.class, PVE.class }) Class<T> type,
			@Values(booleans = { true, false }) boolean single) throws Exception {

		T instance;
		if (single) {
			instance = this.template.save(type.getDeclaredConstructor(String.class).newInstance("Parent"));
		}
		else {
			instance = this.template.saveAll(List.of(type.getDeclaredConstructor(String.class).newInstance("Parent"),
					type.getDeclaredConstructor(String.class).newInstance("Parent2")))
				.get(0);
		}

		assertAllRelationshipsHaveBeenCreated(instance);
	}

	@EnableTransactionManagement
	@ComponentScan
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
