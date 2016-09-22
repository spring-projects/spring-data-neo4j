package org.springframework.data.neo4j.transactions;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author vince
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.data.neo4j.transactions.service")
@EnableTransactionManagement
@EnableNeo4jRepositories
public class ProxiedPrototypeSessionBeanContext {

	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		return new Neo4jTransactionManager(sessionFactory());
	}

	@Bean
	public SessionFactory sessionFactory() {
		return new SessionFactory("org.springframework.data.neo4j.transactions.domain");
	}
}
