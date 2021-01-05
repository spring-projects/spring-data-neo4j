/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.examples.jsr303;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.jsr303.domain.Adult;
import org.springframework.data.neo4j.examples.jsr303.service.AdultService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { WebConfiguration.class, JSR303Tests.JSR303Context.class })
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JSR303Tests extends MultiDriverTestClass {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired private AdultService service;

	@Autowired WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void testCanCreateAnAdult() throws Exception {

		Adult adult = new Adult("Peter", 18);
		String json = objectMapper.writeValueAsString(adult);

		mockMvc.perform(post("/adults").contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
	}

	@Test
	public void testCantCreateAnAdultUnderEighteen() throws Exception {

		Adult adult = new Adult("Peter", 16);
		String json = objectMapper.writeValueAsString(adult);

		mockMvc.perform(post("/adults").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testCantCreateAnAdultWithNoName() throws Exception {

		Adult adult = new Adult(null, 21);
		String json = objectMapper.writeValueAsString(adult);

		mockMvc.perform(post("/adults").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testCantCreateAnAdultWitAShortName() throws Exception {

		Adult adult = new Adult("A", 21);
		String json = objectMapper.writeValueAsString(adult);

		mockMvc.perform(post("/adults").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest());
	}

	@Configuration
	@ComponentScan(basePackageClasses = { AdultService.class })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.jsr303.repo")
	@EnableTransactionManagement
	static class JSR303Context {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.jsr303.domain");
		}
	}

}
