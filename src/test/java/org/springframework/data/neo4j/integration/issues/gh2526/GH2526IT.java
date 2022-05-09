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
package org.springframework.data.neo4j.integration.issues.gh2526;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Andreas Berger
 */
@Neo4jIntegrationTest
public class GH2526IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run("CREATE (o1:Measurand {measurandId: 'o1'})"
					+ "CREATE (acc1:AccountingMeasurementMeta:BaseNodeEntity {nodeId: 'acc1'})"
					+ "CREATE (m1:MeasurementMeta:BaseNodeEntity {nodeId: 'm1'})"
					+ "CREATE (acc1)-[:USES{variable: 'A'}]->(m1)"
					+ "CREATE (o1)-[:IS_MEASURED_BY{ manual: true }]->(acc1)").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	// GH-2526
	void testRichRelationWithInheritance(@Autowired BaseNodeRepository repository) {
		MeasurementProjection m = repository.findByNodeId("acc1", MeasurementProjection.class);
		assertThat(m).isNotNull();
		assertThat(m).extracting(MeasurementProjection::getDataPoints, collection(DataPoint.class))
				.extracting(DataPoint::isManual, DataPoint::getMeasurand).contains(tuple(true, new Measurand("o1")));
	}

	interface BaseNodeFieldsProjection{
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

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
