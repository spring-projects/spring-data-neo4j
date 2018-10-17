package org.springframework.data.neo4j.queries;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.queries.immutable_query_result.ImmutableQueryResult;
import org.springframework.data.neo4j.queries.immutable_query_result.ImmutableQueryResultWithNonFinalFields;
import org.springframework.data.neo4j.queries.immutable_query_result.SomeNodeRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { ImmutableQueryResultTests.ContextConfig.class })
@RunWith(SpringRunner.class)
public class ImmutableQueryResultTests extends MultiDriverTestClass {

	@Autowired private SomeNodeRepository someNodeRepository;

	@Test
	@Ignore
	public void shouldSupportImmutableQueryResults() {
		List<ImmutableQueryResult> x = someNodeRepository.findImmutableQueryResults();
		assertThat(x)
				.flatExtracting(ImmutableQueryResult::getName, ImmutableQueryResult::getaNumber)
				.contains("James Bond", 7L);
	}

	@Test
	@Ignore
	public void shouldNotMutateQueryResultsThroughFieldAccess() {
		List<ImmutableQueryResultWithNonFinalFields> x = someNodeRepository.findImmutableQueryResultsWithNonFinalFields();
		assertThat(x)
				.flatExtracting(ImmutableQueryResultWithNonFinalFields::getName, ImmutableQueryResultWithNonFinalFields::getaNumber)
				.contains("James Bond", 7L);
	}

	private void executeUpdate(String cypher) {
		getGraphDatabaseService().execute(cypher);
	}

	@Configuration
	@ComponentScan(basePackageClasses = SomeNodeRepository.class)
	@EnableNeo4jRepositories(basePackageClasses = SomeNodeRepository.class)
	@EnableTransactionManagement
	static class ContextConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), ImmutableQueryResult.class.getPackage().getName());
		}

	}
}
