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

package org.springframework.data.neo4j.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.domain.User;
import org.springframework.data.neo4j.web.repo.UserRepository;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@WebAppConfiguration
@ContextConfiguration(classes = { WebIntegrationTests.WebAppContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Why is this failing?")
public class WebIntegrationTests extends MultiDriverTestClass {

	@Autowired private UserRepository userRepository;

	@Autowired private WebApplicationContext wac;

	private MockMvc mockMvc;

	private User adam;

	private User daniela;

	private User michal;

	private User vince;

	@Transactional
	@Before
	public void setUp() {
		Mockito.reset(userRepository);

		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

		adam = new User("Adam");
		daniela = new User("Daniela");
		michal = new User("Michal");
		vince = new User("Vince");

		adam.befriend(daniela);
		daniela.befriend(michal);
		michal.befriend(vince);

		// see the @Ignored test below. This doesn't appear to use the same
		// transaction definition as the one created in the test method, even though the tx manager is the same.
		// consequently the tx definition created by the test method is replaced and the test fails.

		// reason: repository is marked as transactional. However, if we remove the @Transactional
		// from the repo and make the test class transactional, the test method
		// shouldNotShareSessionBetweenRequestsWithDifferentSession() fails, because the session is bound
		// to the transaction

		userRepository.save(adam);
	}

	@Test
	public void shouldNotShareSessionBetweenRequestsWithDifferentSession() throws Exception {
		mockMvc.perform(get("/user/{uuid}/friends", adam.getUuid())).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela"));

		mockMvc.perform(get("/user/{uuid}/friends", vince.getUuid())).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Michal"));
	}

	@Test
	public void shouldShareSessionBetweenRequestsDuringSameSession() throws Exception {
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(get("/user/{uuid}/immediateFriends", adam.getUuid()).session(session)).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela"));

		mockMvc.perform(get("/user/{uuid}/immediateFriends", daniela.getUuid()).session(session)).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Adam Michal"));

		mockMvc.perform(get("/user/{uuid}/immediateFriends", michal.getUuid()).session(session)).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela Vince"));

		mockMvc.perform(get("/user/{uuid}/immediateFriends", vince.getUuid()).session(session)).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Michal"));
	}

	@Test
	public void shouldNotShareSessionBetweenMultiThreadedRequests() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 100; i++) {
			final int j = i;
			executor.submit(new Runnable() {
				@Override
				public void run() {
					if (j % 2 == 0) {
						try {
							mockMvc.perform(get("/user/{uuid}/friends", adam.getUuid())).andExpect(status().isOk())
									.andExpect(MockMvcResultMatchers.content().string("Daniela"));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {

						try {
							mockMvc.perform(get("/user/{uuid}/friends", vince.getUuid())).andExpect(status().isOk())
									.andExpect(MockMvcResultMatchers.content().string("Michal"));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.MINUTES);
	}

	@Configuration
	@EnableWebMvc
	@ComponentScan({ "org.springframework.data.neo4j.web.controller", "org.springframework.data.neo4j.web.service" })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.web.repo")
	@EnableTransactionManagement
	static class WebAppContext extends WebMvcConfigurerAdapter {

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
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.web.domain");
		}
	}

}
