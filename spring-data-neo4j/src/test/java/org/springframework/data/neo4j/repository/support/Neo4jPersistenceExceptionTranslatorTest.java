package org.springframework.data.neo4j.repository.support;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Neo4jPersistenceExceptionTranslatorTest.Config.class})
public class Neo4jPersistenceExceptionTranslatorTest extends MultiDriverTestClass {


	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private ApplicationContext context;

	@Test
	public void asdf() {
		Session session = sessionFactory.openSession();
		Filter filter1 = new Filter("firstName", ComparisonOperator.EQUALS, "Gevich");
		Filter filter2 = new Filter("firstName", ComparisonOperator.EQUALS, "Gevich");
		System.out.println(Arrays.toString(context.getBeanDefinitionNames()));
		session.loadAll(User.class, new Filters(filter1, filter2));
	}


	@Configuration
	@EnableNeo4jRepositories("org.springframework.data.neo4j.repository.sample")
	@EnableTransactionManagement
	static class Config {

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager transactionManager()  {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.domain.sample");
		}

		@Bean
		public TransactionTemplate transactionTemplate() {
			return new TransactionTemplate(transactionManager());
		}
	}
}
