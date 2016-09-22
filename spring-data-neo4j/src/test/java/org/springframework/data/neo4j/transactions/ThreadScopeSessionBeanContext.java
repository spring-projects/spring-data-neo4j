package org.springframework.data.neo4j.transactions;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.data.neo4j.transactions.service")
@EnableTransactionManagement
@EnableNeo4jRepositories
public class ThreadScopeSessionBeanContext {

	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		return new Neo4jTransactionManager(sessionFactory());
	}

	@Bean
	public SessionFactory sessionFactory() {
		return new SessionFactory("org.springframework.data.neo4j.transactions.domain");
	}

	@Bean
	public static CustomScopeConfigurer getCustomScopeConfigurer() {

		CustomScopeConfigurer scopeConfigurer = new CustomScopeConfigurer();
		scopeConfigurer.addScope("thread", new SimpleThreadScope());

		return scopeConfigurer;
	}
}
