/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.web.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.WebIntegrationIT;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@Configuration
@EnableWebMvc
@ComponentScan({ "org.springframework.data.neo4j.web.controller", "org.springframework.data.neo4j.web.service" })
@EnableNeo4jRepositories("org.springframework.data.neo4j.web.repo")
@EnableTransactionManagement
public class WebAppContext extends WebMvcConfigurerAdapter {

	@Bean
	public OpenSessionInViewInterceptor openSessionInViewInterceptor() {
		OpenSessionInViewInterceptor openSessionInViewInterceptor = new OpenSessionInViewInterceptor();
		openSessionInViewInterceptor.setSessionFactory(sessionFactory());
		return openSessionInViewInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addWebRequestInterceptor(openSessionInViewInterceptor());
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		return new WebIntegrationIT.DelegatingTransactionManager(new Neo4jTransactionManager(sessionFactory()));
	}

	@Bean
	public SessionFactory sessionFactory() {
		return new SessionFactory("org.springframework.data.neo4j.web.domain");
	}
}
