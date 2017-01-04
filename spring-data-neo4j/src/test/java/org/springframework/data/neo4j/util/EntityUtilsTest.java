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
package org.springframework.data.neo4j.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.context.RepositoriesTestContext;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Eric Spiegelberg
 */
@ContextConfiguration(classes = {RepositoriesTestContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class EntityUtilsTest extends MultiDriverTestClass {

	@Autowired
    private MovieRepository movieRepository;
	
	@Test
	public void isNew() {
	
		Movie movie = new Movie();
		Boolean isNew = EntityUtils.isNew(movie);
		Assert.assertTrue(isNew);
		
		movieRepository.save(movie);
		isNew = EntityUtils.isNew(movie);
		Assert.assertFalse(isNew);
		
	}
	
}
