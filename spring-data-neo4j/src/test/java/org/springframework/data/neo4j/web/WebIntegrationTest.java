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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.web.context.WebAppContext;
import org.springframework.data.neo4j.web.domain.User;
import org.springframework.data.neo4j.web.repo.UserRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {WebAppContext.class})
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class WebIntegrationTest extends MultiDriverTestClass {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private Session session;

	private MockMvc mockMvc;

	@Before
	public void setUp() {

		session.purgeDatabase();

		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();

		User adam = new User("Adam");
		User daniela = new User("Daniela");
		User michal = new User("Michal");
		User vince = new User("Vince");

		adam.befriend(daniela);
		daniela.befriend(michal);
		michal.befriend(vince);

		userRepository.save(adam);
	}

	@Test
	public void shouldNotShareSessionBetweenRequestsWithDifferentSession() throws Exception {
		mockMvc.perform(get("/user/{name}/friends", "Adam"))
				.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela"));

		mockMvc.perform(get("/user/{name}/friends", "Vince"))
				.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Michal"));
	}

	@Test
	public void shouldShareSessionBetweenRequestsDuringSameSession() throws Exception {
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(get("/user/{name}/immediateFriends", "Adam").session(session))
				.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela"));

		mockMvc.perform(get("/user/{name}/immediateFriends", "Daniela").session(session))
				.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Adam Michal"));

		mockMvc.perform(get("/user/{name}/immediateFriends", "Michal").session(session))
				.andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("Daniela Vince"));

		mockMvc.perform(get("/user/{name}/immediateFriends", "Vince").session(session))
				.andExpect(status().isOk())
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
							mockMvc.perform(get("/user/{name}/friends", "Adam"))
									.andExpect(status().isOk())
									.andExpect(MockMvcResultMatchers.content().string("Daniela"));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {

						try {
							mockMvc.perform(get("/user/{name}/friends", "Vince"))
									.andExpect(status().isOk())
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
}
