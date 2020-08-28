/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.conversion;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.conversion.ogm618.MyNode;
import org.springframework.data.neo4j.conversion.ogm618.MyNodeRepository;
import org.springframework.data.neo4j.conversion.ogm618.ResultHolder;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { Neo4jOgmEntityInstantiatorAdapterTests.ContextConfig.class })
@RunWith(SpringRunner.class)
@DirtiesContext
public class Neo4jOgmEntityInstantiatorAdapterTests {

	private static final Config driverConfig = Config.build().withoutEncryption().toConfig();

	private static ServerControls serverControls;
	private static URI boltURI;

	@BeforeClass
	public static void initializeNeo4j() {

		serverControls = TestServerBuilders.newInProcessBuilder()
				.withProcedure(Neo4jOgmEntityInstantiatorAdapterTests.ListReturningThing.class)
				.withFixture("CREATE (m:MyNode{name: 'All the', things: []})").newServer();
		boltURI = serverControls.boltURI();
	}

	@Autowired
	private MyNodeRepository myNodeRepository;

	@Test
	public void ctorShouldHandleEmptyArrayFromAttributes() {

		Optional<MyNode> optionalNode = myNodeRepository.findOneByName("All the");
		assertThat(optionalNode).isPresent().get().extracting(MyNode::getThings).asList().isEmpty();
	}

	@Test
	public void ctorShouldHandleEmptyArrayFromStoredProcedure() {

		List<ResultHolder> resultHolders = myNodeRepository.generateListOfNodes(false);
		assertThat(resultHolders).isNotNull().hasSize(1);

		assertThat(resultHolders.get(0).getResult()).isNotNull().hasSize(1);
	}

	@Test
	public void ctorShouldHandleNonEmptyListFromStoredProcedure() {

		List<ResultHolder> resultHolders = myNodeRepository.generateListOfNodes(true);
		assertThat(resultHolders).isNotNull().hasSize(1);

		assertThat(resultHolders.get(0).getResult()).isNotNull().isEmpty();
	}

	public static class ListReturningThing {

		@Context
		public GraphDatabaseService db;

		@Procedure("test.generateListOfNodes")
		public Stream<ListOfNodesResult> generateListOfNodes(@Name("empty") boolean empty) {
			List<Node> result = new ArrayList<>();
			if (!empty) {
				db.findNodes(Label.label("MyNode")).forEachRemaining(result::add);
			}
			return Stream.of(new ListOfNodesResult(result));
		}

		public static class ListOfNodesResult {
			public List<Node> result;

			public ListOfNodesResult(List<Node> result) {
				this.result = result;
			}
		}
	}

	@Configuration
	@ComponentScan(basePackageClasses = ResultHolder.class)
	@EnableNeo4jRepositories(basePackageClasses = ResultHolder.class)
	@EnableTransactionManagement
	static class ContextConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {

			return new SessionFactory(new BoltDriver(GraphDatabase.driver(boltURI, driverConfig)),
					ResultHolder.class.getPackage().getName());
		}
	}
}
