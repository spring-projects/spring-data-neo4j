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
package org.springframework.data.neo4j.examples.jsr303;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.jsr303.domain.Adult;
import org.springframework.data.neo4j.examples.jsr303.service.AdultService;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { WebConfiguration.class, JSR303Tests.JSR303Context.class })
@WebAppConfiguration
@RunWith(SpringRunner.class)
public class JSR303Tests {

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
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.examples.jsr303.domain",
			repositoryPackages = "org.springframework.data.neo4j.examples.jsr303.repo")
	@ComponentScan(basePackageClasses = AdultService.class)
	static class JSR303Context {}
}
