/*
 * Copyright 2011-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Neo4jIntegrationTest
class CascadingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	@ComponentScan
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	@Autowired
	private Neo4jTemplate template;

	@Autowired
	private Driver driver;

	@BeforeAll
	static void clean(@Autowired Driver driver) {
		driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
	}

	@ParameterizedTest
	@ValueSource(classes = {PUI.class, PUE.class, PVI.class, PVE.class})
	<T extends Parent> void newItemsMustBePersistedRegardlessOfCascadeSingleSave(Class<T> type) throws Exception {

		T instance = template.save(type.getDeclaredConstructor(String.class).newInstance("Parent"));

		try (var session = driver.session()) {
			var result = session.run("""
							MATCH (p:%s)
							MATCH (p) -[:HAS_SINGLE_CUI]-> (sCUI)
							MATCH (p) -[:HAS_SINGLE_CUE]-> (sCUE)
							MATCH (p) -[:HAS_MANY_CUI]->   (mCUI)
							MATCH (p) -[:HAS_SINGLE_CVI]-> (sCVI)
							MATCH (p) -[:HAS_SINGLE_CVE]-> (sCVE)
							MATCH (p) -[:HAS_MANY_CVI]->    (mCVI)
							MATCH (sCUI) -[:HAS_NESTED_CHILDREN]-> (nc1)
							MATCH (mCUI) -[:HAS_NESTED_CHILDREN]-> (nc2)
							RETURN p, sCUI, sCUE, collect(DISTINCT mCUI) AS mCUI, collect(DISTINCT nc1) AS nc1, collect(DISTINCT nc2) AS nc2,
							          sCVI, sCVE, collect(DISTINCT mCVI) AS mCVI
							""".formatted(type.getSimpleName()))
					.list();

			assertThat(result).hasSize(1).element(0)
					.satisfies(r -> {
						if (instance instanceof Versioned) {
							assertThat(r.get("p").asNode().get("version").asLong()).isZero();
						}
						if (instance instanceof ExternalId) {
							assertThat(r.get("p").asNode().get("id").asString()).isEqualTo(instance.getId());
						} else {
							assertThat(r.get("p").asNode().elementId()).isEqualTo(instance.getId());
						}
						assertThat(r.get("sCUI").hasType(TypeSystem.getDefault().NODE())).isTrue();
						assertThat(r.get("sCUE").hasType(TypeSystem.getDefault().NODE())).isTrue();
						assertThat(r.get("mCUI").asList(v -> v.asNode().get("name").asString()))
								.containsExactlyInAnyOrder("Parent.cUI1", "Parent.cUI2");
						assertThat(r.get("nc1").asList(v -> v.asNode().get("name").asString()))
								.containsExactlyInAnyOrder("Parent.singleCUI.cc1", "Parent.singleCUI.cc2");
						assertThat(r.get("nc2").asList(v -> v.asNode().get("name").asString()))
								.containsExactlyInAnyOrder("Parent.cUI1.cc1", "Parent.cUI1.cc2", "Parent.cUI2.cc1", "Parent.cUI2.cc2");
						assertThat(r.get("sCVI").asNode().get("version").asLong()).isZero();
						assertThat(r.get("sCVE").asNode().get("version").asLong()).isZero();
						assertThat(r.get("mCVI").asList(v -> {
							var node = v.asNode();
							return node.get("name").asString() + "." + node.get("version").asLong();
						}))
								.containsExactlyInAnyOrder("Parent.cVI1.0", "Parent.cVI2.0");
					});
		}
	}

	// Left todo
	// write actual code
	// Test that the update doesn't update
	// just the reactive part
	// and the circles, done.
}