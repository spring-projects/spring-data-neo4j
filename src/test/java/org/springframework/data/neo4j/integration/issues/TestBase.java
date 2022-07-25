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
package org.springframework.data.neo4j.integration.issues;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.issues.gh2328.Entity2328;
import org.springframework.data.neo4j.integration.issues.gh2347.Application;
import org.springframework.data.neo4j.integration.issues.gh2347.Workflow;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;

abstract class TestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static UUID idOfAnEntity2328;

	@BeforeEach
	protected final void beforeEach(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			List<String> labelsToDelete = List.of("AbstractBase", "AccountingMeasurementMeta", "Application",
					"BaseNodeEntity", "CityModel", "ConcreteImplementationOne", "ConcreteImplementationTwo",
					"Credential", "Device",
					"DomainModel", "GH2533Entity", "Measurand", "MeasurementMeta", "SomethingInBetween", "SpecialKind",
					"Vertex");

			// Detach delete things
			transaction.run("""
							MATCH (n) WHERE any(label IN  labels(n) WHERE label in $labels )
							DETACH DELETE n
							""",
					Map.of("labels", labelsToDelete)
			).consume();
			transaction.run("MATCH ()- [r:KNOWS]-() DELETE r").consume();

			// 2498
			transaction.run(
							"UNWIND ['A', 'B', 'C'] AS name WITH name CREATE (n:DomainModel {id: randomUUID(), name: name})")
					.consume();
			transaction.run("CREATE (n:Vertex {name: 'a'}) -[:CONNECTED_TO] ->(m:Vertex {name: 'b'})").consume();

			// 2498/2500
			transaction.run("CREATE (d:Device {id: 1, name:'Testdevice', version:0})").consume();

			// 2526
			transaction.run("""
					CREATE (o1:Measurand {measurandId: 'o1'})
					CREATE (acc1:AccountingMeasurementMeta:MeasurementMeta:BaseNodeEntity {nodeId: 'acc1'})
					CREATE (m1:MeasurementMeta:BaseNodeEntity {nodeId: 'm1'})
					CREATE (acc1)-[:USES{variable: 'A'}]->(m1)
					CREATE (o1)-[:IS_MEASURED_BY{ manual: true }]->(acc1)
					"""
			).consume();

			// 2415
			transaction.run("""
					CREATE (root:NodeEntity:BaseNodeEntity{nodeId: 'root'})
					CREATE (company:NodeEntity:BaseNodeEntity{nodeId: 'comp'})
					CREATE (cred:Credential{id: 'uuid-1', name: 'Creds'})
					CREATE (company)-[:CHILD_OF]->(root)
					CREATE (root)-[:HAS_CREDENTIAL]->(cred)
					CREATE (company)-[:WITH_CREDENTIAL]->(cred)
					""");

			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	protected static void setupGH2289(QueryRunner queryRunner) {
		for (int i = 0; i < 4; ++i) {
			queryRunner.run("CREATE (s:SKU_RO {number: $i, name: $n})",
					Values.parameters("i", i, "n", new String(new char[] { (char) ('A' + i) }))).consume();
		}
	}

	protected static void setupGH2328(QueryRunner queryRunner) {
		idOfAnEntity2328 = UUID.fromString(
				queryRunner.run("CREATE (f:Entity2328 {name: 'A name', id: randomUUID()}) RETURN f.id").single()
						.get(0).asString());
	}

	protected static void setupGH2572(QueryRunner queryRunner) {
		queryRunner.run("CREATE (p:GH2572Parent {id: 'GH2572Parent-1', name:'no-pets'})");
		queryRunner.run("CREATE (p:GH2572Parent {id: 'GH2572Parent-2', name:'one-pet'}) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-3', name: 'a-pet'})");
		queryRunner.run("MATCH (p:GH2572Parent {id: 'GH2572Parent-2'}) CREATE (p) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-4', name: 'another-pet'})");
	}

	protected static void assertLabels(BookmarkCapture bookmarkCapture, List<String> ids) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			for (String id : ids) {
				List<String> labels = session.readTransaction(
						tx -> tx.run("MATCH (n) WHERE n.id = $id RETURN labels(n)", Collections.singletonMap("id", id))
								.single().get(0).asList(
										Value::asString));
				assertThat(labels)
						.hasSize(3)
						.contains("AbstractLevel2", "AbstractLevel3")
						.containsAnyOf("Concrete1", "Concrete2");

			}
		}
	}

	protected static boolean requirements(Entity2328 someEntity) {
		assertThat(someEntity).isNotNull();
		assertThat(someEntity).extracting(Entity2328::getId).isEqualTo(idOfAnEntity2328);
		assertThat(someEntity).extracting(Entity2328::getName).isEqualTo("A name");
		return true;
	}

	protected static Application createData() {

		Application app1 = new Application("app-1");
		Workflow wf1 = new Workflow("wf-1");
		Workflow wf2 = new Workflow("wf-2");

		wf1.setApplication(app1);
		wf2.setApplication(app1);

		app1.getWorkflows().addAll(Arrays.asList(wf1, wf2));
		return app1;
	}

	protected static void createData(BiConsumer<List<Application>, List<Workflow>> actualTest) {

		Application app1 = new Application("app-1");
		Workflow wf1 = new Workflow("wf-1");
		wf1.setApplication(app1);
		app1.getWorkflows().add(wf1);

		Application app2 = new Application("app-2");
		Workflow wf2 = new Workflow("wf-2");
		wf2.setApplication(app2);
		app2.getWorkflows().add(wf2);

		actualTest.accept(Arrays.asList(app1, app2), Arrays.asList(wf1, wf2));
	}

	protected static void assertSingleApplicationNodeWithMultipleWorkflows(Driver driver,
			BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Record record = session.readTransaction(
					tx -> tx.run("MATCH (a:Application)-->(w) RETURN a, collect(w) as workflows").single());
			assertThat(record.get("a").asNode().get("id").asString()).isEqualTo("app-1");
			assertThat(record.get("workflows").asList(v -> v.asNode().get("id").asString())).containsExactlyInAnyOrder(
					"wf-1", "wf-2");
		}
	}

	protected static void assertMultipleApplicationsNodeWithASingleWorkflow(Driver driver,
			BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> records = session.readTransaction(
					tx -> tx.run("MATCH (a:Application)-->(w) RETURN a, collect(w) as workflows ORDER by a.id ASC")
							.list());
			assertThat(records).hasSize(2);
			assertThat(records.get(0).get("a").asNode().get("id").asString()).isEqualTo("app-1");
			assertThat(records.get(0).get("workflows")
					.asList(v -> v.asNode().get("id").asString())).containsExactlyInAnyOrder("wf-1");
			assertThat(records.get(1).get("a").asNode().get("id").asString()).isEqualTo("app-2");
			assertThat(records.get(1).get("workflows")
					.asList(v -> v.asNode().get("id").asString())).containsExactlyInAnyOrder("wf-2");
		}
	}
}
