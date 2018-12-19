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
package org.springframework.data.neo4j.extensions;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: Vince Bickers
 */
@ContextConfiguration(classes = CustomGraphRepositoryTests.CustomPersistenceContext.class)
@RunWith(SpringRunner.class)
public class CustomGraphRepositoryTests {

	@Autowired private UserRepository repository;

	/**
	 * asserts that the correct proxied object is created by Spring and that we can integrate with it.
	 */
	@Test
	public void shouldExposeCommonMethodOnExtendedRepository() {
		assertTrue(repository.sharedCustomMethod());
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.extensions.domain")
	@EnableNeo4jRepositories(repositoryBaseClass = CustomGraphRepositoryImpl.class)
	static class CustomPersistenceContext {}
}
