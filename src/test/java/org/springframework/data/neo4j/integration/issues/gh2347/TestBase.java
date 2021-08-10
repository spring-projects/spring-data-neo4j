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
package org.springframework.data.neo4j.integration.issues.gh2347;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 * @soundtrack David Hasselhoff - Open Your Eyes
 */
abstract class TestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static UUID id;

	@BeforeEach
	protected void setupData() {

		try (Session session = neo4jConnectionSupport.getDriver().session()) {
			session.writeTransaction(tx -> tx.run("MATCH (n) detach delete n"));
		}
	}

	protected final Application createData() {

		Application app1 = new Application("app-1");
		Workflow wf1 = new Workflow("wf-1");
		Workflow wf2 = new Workflow("wf-2");

		wf1.setApplication(app1);
		wf2.setApplication(app1);

		app1.getWorkflows().addAll(Arrays.asList(wf1, wf2));
		return app1;
	}

	protected final void assertDatabase(Driver driver) {

		try (Session session = driver.session()) {
			Record record = session.readTransaction(
					tx -> tx.run("MATCH (a:Application)-->(w) RETURN a, collect(w) as workflows").single());
			assertThat(record.get("a").asNode().get("id").asString()).isEqualTo("app-1");
			assertThat(record.get("workflows").asList(v -> v.asNode().get("id").asString())).containsExactlyInAnyOrder(
					"wf-1", "wf-2");
		}
	}
}
