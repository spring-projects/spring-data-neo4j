/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transactions;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.LocalSessionFactoryBean;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.transaction.support.SpringSessionProxyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Vince Bickers
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.data.neo4j.transactions",
		excludeFilters =@ComponentScan.Filter(
				type = FilterType.REGEX,
				pattern = "org\\.springframework\\.data\\.neo4j\\.transactions\\.TransactionalEventListenerTests.*"))
@EnableTransactionManagement
@EnableNeo4jRepositories
public class ApplicationConfig {

	@Bean
	public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) throws Exception {
		return new Neo4jTransactionManager(sessionFactory);
	}

	@Bean
	public SessionFactory sessionFactory() throws Exception {
		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setPackagesToScan("org.springframework.data.neo4j.transactions");
		lsfb.afterPropertiesSet();
		return lsfb.getObject();
	}


	@Bean
	public Neo4jTemplate neo4jTemplate(Session session) throws Exception {
		return new Neo4jTemplate(session);
	}

	@Bean
	public Session getSession(SessionFactory sessionFactory) throws Exception {
		SpringSessionProxyBean proxy = new SpringSessionProxyBean();
		proxy.setSessionFactory(sessionFactory);
		proxy.afterPropertiesSet();
		return proxy.getObject();
	}
}
