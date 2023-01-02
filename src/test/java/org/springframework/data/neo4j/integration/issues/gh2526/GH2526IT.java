/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2526;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Test setup for GH-2526: Query creation of and picking the correct named relationship out of the result.
 */
@Neo4jIntegrationTest
public class GH2526IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run(
					"CREATE (o1:Measurand {measurandId: 'o1'})\n" +
					"CREATE (acc1:AccountingMeasurementMeta:MeasurementMeta:BaseNodeEntity {nodeId: 'acc1'})\n" +
					"CREATE (m1:MeasurementMeta:BaseNodeEntity {nodeId: 'm1'})\n" +
					"CREATE (acc1)-[:USES{variable: 'A'}]->(m1)\n" +
					"CREATE (o1)-[:IS_MEASURED_BY{ manual: true }]->(acc1)\n"
			).consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void relationshipWillGetFoundInResultOfMultilevelInheritance(@Autowired BaseNodeRepository repository) {
		MeasurementProjection m = repository.findByNodeId("acc1", MeasurementProjection.class);
		assertThat(m).isNotNull();
		assertThat(m.getDataPoints()).isNotEmpty();
		assertThat(m).extracting(MeasurementProjection::getDataPoints, InstanceOfAssertFactories.collection(DataPoint.class))
				.extracting(DataPoint::isManual, DataPoint::getMeasurand).contains(tuple(true, new Measurand("o1")));
	}

	interface BaseNodeFieldsProjection {
		String getNodeId();
	}


	interface MeasurementProjection extends BaseNodeFieldsProjection {
		Set<DataPoint> getDataPoints();
		Set<VariableProjection> getVariables();
	}

	interface VariableProjection {
		BaseNodeFieldsProjection getMeasurement();
		String getVariable();
	}


	/**
	 * Defining most concrete entity
	 */
	@Node
	@Data
	@Setter(AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
	@SuperBuilder(toBuilder = true)
	public static class AccountingMeasurementMeta extends MeasurementMeta {

		private String formula;

		@Relationship(type = "WEIGHTS", direction = Relationship.Direction.OUTGOING)
		private MeasurementMeta baseMeasurement;
	}

	/**
	 * Defining base entity
	 */
	@Node
	@NonFinal
	@Data
	@Setter(AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	@SuperBuilder(toBuilder = true)
	public static class BaseNodeEntity {

		@Id
		@GeneratedValue(UUIDStringGenerator.class)
		@EqualsAndHashCode.Include
		private String nodeId;
	}

	/**
	 * Target node
	 */
	@Node
	@Value
	@AllArgsConstructor
	@Immutable
	public static class Measurand {

		@Id
		String measurandId;
	}

	/**
	 * Defining relationship to measurand
	 */
	@Node
	@Data
	@Setter(AccessLevel.PRIVATE)
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
	@SuperBuilder(toBuilder = true)
	public static class MeasurementMeta extends BaseNodeEntity {

		@Relationship(type = "IS_MEASURED_BY", direction = Relationship.Direction.INCOMING)
		private Set<DataPoint> dataPoints;

		@Relationship(type = "USES", direction = Relationship.Direction.OUTGOING)
		private Set<Variable> variables;
	}

	/**
	 * Second type of relationship
	 */
	@RelationshipProperties
	@Value
	@With
	@AllArgsConstructor
	@EqualsAndHashCode
	@Immutable
	public static class Variable {
		@RelationshipId
		Long id;

		@TargetNode
		MeasurementMeta measurement;

		String variable;

		public static Variable create(MeasurementMeta measurement, String variable) {
			return new Variable(null, measurement, variable);
		}

		@Override
		public String toString() {
			return variable + ": " + measurement.getNodeId();
		}
	}

	/**
	 * Relationship with properties between measurement and measurand
	 */
	@RelationshipProperties
	@Value
	@With
	@AllArgsConstructor
	@Immutable
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	public static class DataPoint {

		@RelationshipId
		Long id;

		boolean manual;

		@TargetNode
		@EqualsAndHashCode.Include
		Measurand measurand;
	}

	interface BaseNodeRepository extends Neo4jRepository<BaseNodeEntity, String> {
		<R> R findByNodeId(String nodeIds, Class<R> clazz);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
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
