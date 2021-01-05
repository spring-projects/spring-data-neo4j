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
package org.springframework.data.neo4j.queries;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.driver.Config;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.queries.stored_procedures.DocumentEntity;
import org.springframework.data.neo4j.queries.stored_procedures.DocumentRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { StoredProceduresTests.ContextConfig.class })
@RunWith(SpringRunner.class)
public class StoredProceduresTests {

	private static final Config driverConfig = Config.builder().withoutEncryption().build();

	private static ServerControls serverControls;
	private static URI boltURI;

	@BeforeClass
	public static void initializeNeo4j() {

		serverControls = TestServerBuilders.newInProcessBuilder().withProcedure(ApocLovesSwitch.class).newServer();
		boltURI = serverControls.boltURI();
	}

	@Autowired private DocumentRepository documentRepository;

	@Test // DATAGRAPH-1134
	public void atQueryMethodShouldIgnoreResultsOfStoredProceduresWhenMappedToNull() {
		documentRepository.callApocProcedureAndIgnoreResult();
	}

	@AfterClass
	public static void tearDownNeo4j() {
		serverControls.close();
	}

	public static class ApocLovesSwitch {
		@Procedure(name = "apoc.periodic.iterate", mode = Mode.WRITE)
		public Stream<BatchAndTotalResult> iterate(@Name("cypherIterate") String cypherIterate,
				@Name("cypherAction") String cypherAction, @Name("config") Map<String, Object> config) {
			return Stream.of(new BatchAndTotalResult());
		}
	}

	public static class BatchAndTotalResult {
		public final long batches;
		public final long total;
		public final long timeTaken;

		public BatchAndTotalResult() {

			ThreadLocalRandom random = ThreadLocalRandom.current();
			this.batches = random.nextLong();
			this.total = random.nextLong();
			this.timeTaken = random.nextLong();
		}
	}

	@Configuration
	@ComponentScan(basePackageClasses = DocumentEntity.class)
	@EnableNeo4jRepositories(basePackageClasses = DocumentRepository.class)
	@EnableTransactionManagement
	static class ContextConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {

			return new SessionFactory(new BoltDriver(GraphDatabase.driver(boltURI, driverConfig)),
					DocumentEntity.class.getName());
		}
	}
}
