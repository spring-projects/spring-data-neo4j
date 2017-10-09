package org.springframework.data.neo4j.namedquery;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.neo4j.namedquery.domain.SampleEntityForNamedQuery;
import org.springframework.data.neo4j.namedquery.repo.SampleEntityForNamedQueryRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(classes = { NamedQueryTests.NamedQueryContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class NamedQueryTests extends MultiDriverTestClass {

	private static final String SAMPLE_ENTITY_NAME = "test";

	@Autowired private SampleEntityForNamedQueryRepository repository;

	@Test
	public void findElementByQueryAnnotation() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.getTitleEntity();
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByDerivedFunction() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByName(SAMPLE_ENTITY_NAME);
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByNamedQuery() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithoutParameter();
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByNamedQueryWithParameter() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithParameter(SAMPLE_ENTITY_NAME);
		assertNotNull(titleEntity);
	}

	private void createAndSaveSampleEntity() {
		SampleEntityForNamedQuery entity = new SampleEntityForNamedQuery();
		entity.setName(SAMPLE_ENTITY_NAME);
		repository.save(entity);
	}

	@org.springframework.context.annotation.Configuration
	@ComponentScan({ "org.springframework.data.neo4j.namedquery" })
	@EnableNeo4jRepositories(value = "org.springframework.data.neo4j.namedquery.repo")
	@EnableTransactionManagement
	static class NamedQueryContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			Configuration.Builder builder = getBaseConfiguration();

			Configuration configuration = builder.build();

			return new SessionFactory(configuration, "org.springframework.data.neo4j.namedquery.domain");
		}
	}
}
